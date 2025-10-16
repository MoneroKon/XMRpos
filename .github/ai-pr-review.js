import { Octokit } from "@octokit/rest";
import fs from "node:fs";
import fetch from "node-fetch";

const {
  GITHUB_REPOSITORY,
  GITHUB_EVENT_PATH,
  GITHUB_TOKEN,
  HYPERBOLIC_API_KEY,
  HYPERBOLIC_BASE_URL = "https://api.hyperbolic.xyz/v1",
  MODEL_SUMMARY = "Qwen/Qwen2.5-Coder-32B-Instruct",
  MODEL_REVIEW = "Qwen/Qwen2.5-Coder-32B-Instruct",
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

async function callLLM(model, messages, max_tokens = 1200, temperature = 0.2) {
  const res = await fetch(`${HYPERBOLIC_BASE_URL}/chat/completions`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${HYPERBOLIC_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ model, messages, temperature, max_tokens }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  const json = await res.json();
  let raw = (json.choices?.[0]?.message?.content ?? "").trim();
  const m = raw.match(/<final>\s*([\s\S]*?)\s*<\/final>/i);
  if (m) raw = m[1].trim();
  if (/deepseek/i.test(model)) {
    if (!m && raw.includes("</think>")) raw = raw.split("</think>").pop();
    raw = raw.replace(/<think>[\s\S]*?<\/think>/gi, "");
  }
  return raw.replace(/\n{3,}/g, "\n\n").trim();
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

const planPrompt = `
Summarize this pull request:
- Purpose and intent
- Key files changed

Title: ${event.pull_request.title}
Body: ${event.pull_request.body || "(no description)"}
Files:
${fileList}
`;

const plan = await callLLM(MODEL_SUMMARY, [
  { role: "system", content: "You are a senior software engineer. Return only <final>...</final> with exactly four sections titled: Purpose, Key Files, Risks, Deep Review. Use bullet points. Max 4 bullets per section. Max 12 words per bullet. No preamble. No meta commentary. No repetition." },
  { role: "user", content: planPrompt },
], 500, 0.1);

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
      { role: "system", content: "You are a rigorous code reviewer. Respond with terse, actionable feedback. Return only the final answer wrapped in <final>...</final>. Do not include hidden reasoning." },
      { role: "user", content: prompt },
    ], 1500, 0.15);
    reviews.push(`### Chunk ${i + 1}\n${text}`);
  } catch (e) {
    reviews.push(`### Chunk ${i + 1}\nError: ${e.message}`);
  }
}

const synthesis = await callLLM(MODEL_SUMMARY, [
  { role: "system", content: "Merge and deduplicate findings. Return only <final>...</final> with sections: Summary, Key Issues, Recommendations. Use bullet points. Max 5 bullets per section. Max 12 words per bullet. No preamble. No meta commentary." },
  { role: "user", content: reviews.join("\n\n") },
], 600, 0.15);

const safe = s => (s && s.trim()) ? s.trim() : "(model returned no visible content)";

await octokit.issues.createComment({
  owner,
  repo,
  issue_number: prNumber,
  body: `🤖 **AI PR Summary** (model: \`${MODEL_SUMMARY}\`)\n\n${safe(plan)}`,
});
await octokit.issues.createComment({
  owner,
  repo,
  issue_number: prNumber,
  body: `🤖 **AI Code Review** (model: \`${MODEL_REVIEW}\`)\n\n${safe(synthesis)}`,
});

console.log("AI PR summary and review comments posted successfully.");
