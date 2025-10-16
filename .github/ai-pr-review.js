import { Octokit } from "@octokit/rest";
import fs from "node:fs";
import fetch from "node-fetch";

const {
  GITHUB_REPOSITORY,
  GITHUB_EVENT_PATH,
  GITHUB_TOKEN,
  HYPERBOLIC_API_KEY,
  HYPERBOLIC_BASE_URL = "https://api.hyperbolic.xyz/v1",
  MODEL_SUMMARY = "deepseek-ai/DeepSeek-R1-0528",
  MODEL_REVIEW  = "Qwen/Qwen2.5-Coder-32B-Instruct",
} = process.env;

if (!HYPERBOLIC_API_KEY) {
  console.error("HYPERBOLIC_API_KEY is empty — secrets unavailable?");
  process.exit(1);
}
if (!GITHUB_TOKEN) {
  console.error("GITHUB_TOKEN is empty — cannot post PR comments.");
  process.exit(1);
}

const event = JSON.parse(fs.readFileSync(GITHUB_EVENT_PATH, "utf8"));
const [owner, repo] = GITHUB_REPOSITORY.split("/");
const prNumber = event.pull_request?.number;
if (!prNumber) {
  console.error("No pull_request.number found in event payload");
  process.exit(1);
}

const octokit = new Octokit({ auth: GITHUB_TOKEN });

// --- Fetch data safely (no checkout, safe for forks) ---
async function fetchDiff() {
  const res = await octokit.request("GET /repos/{owner}/{repo}/pulls/{pull_number}", {
    owner,
    repo,
    pull_number: prNumber,
    headers: { accept: "application/vnd.github.v3.diff" },
  });
  return res.data;
}

async function listFiles() {
  const res = await octokit.pulls.listFiles({
    owner,
    repo,
    pull_number: prNumber,
    per_page: 300,
  });
  return res.data;
}

// --- Call Hyperbolic API ---
function sanitizeLLM(text = "") {
  text = text.replace(/<think>[\s\S]*?<\/think>/gi, "");
  text = text.replace(/^\s*(?:Thought|Chain-of-thought|Reasoning|Thinking):.*$/gim, "");
  return text.trim().replace(/\n{3,}/g, "\n\n");
}

async function callLLM(model, messages, max_tokens = 1200, temperature = 0.2) {
  const res = await fetch(`${HYPERBOLIC_BASE_URL}/chat/completions`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${HYPERBOLIC_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      messages,
      temperature,
      max_tokens,
      stop: ["<think>", "</think>", "Thought:", "Chain-of-thought:", "Reasoning:", "Thinking:"],
    }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  const json = await res.json();
  const raw = json.choices?.[0]?.message?.content?.trim() || "";
  return sanitizeLLM(raw);
}

function chunk(text, size = 12000) {
  const out = [];
  for (let i = 0; i < text.length; i += size) out.push(text.slice(i, i + size));
  return out;
}

const diff = await fetchDiff();
const files = await listFiles();
const chunks = chunk(diff);
const fileList = files.map(f => `${f.filename} (+${f.additions}/-${f.deletions})`).join("\n");

// --- Stage A: Summarize the PR ---
const planPrompt = `
Summarize this pull request:
- Purpose and intent
- Key files changed
- Risks, side effects, or performance/security implications
- Which files need detailed review

Title: ${event.pull_request.title}
Body: ${event.pull_request.body || "(no description)"}
Files:
${fileList}
`;

const plan = await callLLM(MODEL_SUMMARY, [
  { role: "system", content: "You are a senior software engineer producing concise, structured summaries." },
  { role: "user", content: planPrompt },
], 900, 0.1);

// --- Stage B: Review each diff chunk ---
const reviews = [];
for (let i = 0; i < chunks.length; i++) {
  const prompt = `
Review this diff chunk.
- Identify correctness, security, or performance issues.
- Suggest precise improvements or safer alternatives.

Context:
${plan}

Diff chunk ${i + 1}:
${chunks[i]}
`;
  try {
    const text = await callLLM(MODEL_REVIEW, [
      { role: "system", content: "You are a rigorous code reviewer. Respond with terse, actionable feedback." },
      { role: "user", content: prompt },
    ], 1400, 0.15);
    reviews.push(`### Chunk ${i + 1}\n${text}`);
  } catch (e) {
    reviews.push(`### Chunk ${i + 1}\nError: ${e.message}`);
  }
}

// --- Stage C: Combine the review results ---
const synthesis = await callLLM(MODEL_SUMMARY, [
  { role: "system", content: "Merge and deduplicate findings. Output short sections: Summary, Key Issues, Recommendations, Suggested Tests." },
  { role: "user", content: reviews.join("\n\n") },
], 1200, 0.15);

// --- Post comments back to PR ---
await octokit.issues.createComment({
  owner,
  repo,
  issue_number: prNumber,
  body: `🤖 **AI PR Summary** (model: \`${MODEL_SUMMARY}\`)\n\n${plan}`,
});
await octokit.issues.createComment({
  owner,
  repo,
  issue_number: prNumber,
  body: `🤖 **AI Code Review** (model: \`${MODEL_REVIEW}\`)\n\n${synthesis}`,
});

console.log("AI PR summary and review comments posted successfully.");
