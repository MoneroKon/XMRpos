// .github/ai-review.js
// Requires: node >= 18, node-fetch@3, @octokit/rest@20
import { Octokit } from "@octokit/rest";
import fs from "node:fs";

// --- Environment ---
const {
  GITHUB_REPOSITORY,
  GITHUB_EVENT_PATH,
  GITHUB_TOKEN,
  HYPERBOLIC_API_KEY,
  HYPERBOLIC_BASE_URL = "https://api.hyperbolic.xyz/v1",
  MODEL_SUMMARY = "deepseek-ai/DeepSeek-R1-0528",
  MODEL_REVIEW = "qwen/Qwen3-Coder-480B-A35B-Instruct",
} = process.env;

if (!HYPERBOLIC_API_KEY) {
  console.error("HYPERBOLIC_API_KEY not found.");
  process.exit(1);
}
if (!GITHUB_TOKEN) {
  console.error("GITHUB_TOKEN not found.");
  process.exit(1);
}

const event = JSON.parse(fs.readFileSync(GITHUB_EVENT_PATH, "utf8"));
const [owner, repo] = GITHUB_REPOSITORY.split("/");
const prNumber = event.pull_request?.number;
if (!prNumber) {
  console.error("No PR number found in event.");
  process.exit(1);
}

const octokit = new Octokit({ auth: GITHUB_TOKEN });

// --- Helpers ---
const chunkText = (text, size = 12000) => {
  const out = [];
  for (let i = 0; i < text.length; i += size) out.push(text.slice(i, i + size));
  return out;
};

async function callLLM(model, messages, max_tokens = 1200, temperature = 0.2) {
  const res = await fetch(`${HYPERBOLIC_BASE_URL}/chat/completions`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${HYPERBOLIC_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ model, messages, max_tokens, temperature }),
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(`LLM error ${res.status}: ${err}`);
  }
  const json = await res.json();
  return json.choices?.[0]?.message?.content?.trim() || "";
}

// --- Gather PR data ---
const prInfo = await octokit.pulls.get({
  owner,
  repo,
  pull_number: prNumber,
});
const prDiffResp = await octokit.request(
  "GET /repos/{owner}/{repo}/pulls/{pull_number}",
  {
    owner,
    repo,
    pull_number: prNumber,
    headers: { accept: "application/vnd.github.v3.diff" },
  }
);
const files = await octokit.pulls.listFiles({
  owner,
  repo,
  pull_number: prNumber,
  per_page: 300,
});

const diff = prDiffResp.data;
const fileList = files.data
  .map(f => `${f.filename} (+${f.additions}/-${f.deletions})`)
  .join("\n");

// --- Stage 1: PR Summary and Plan ---
const summaryPrompt = `
You are a senior engineer summarizing a pull request.

Goal:
- Summarize the intent of the PR.
- Identify key files and modules changed.
- Note risks, breaking changes, and areas needing deeper inspection.

Output in concise Markdown paragraphs.

PR Title: ${prInfo.data.title}
PR Description: ${prInfo.data.body || "(none)"}

Changed Files:
${fileList}
`;

console.log("Generating summary plan...");
const plan = await callLLM(MODEL_SUMMARY, [
  { role: "system", content: "You are an expert PR summarizer." },
  { role: "user", content: summaryPrompt },
], 900, 0.15);

// --- Stage 2: Detailed Code Review ---
const chunks = chunkText(diff);
const reviews = [];
for (let i = 0; i < chunks.length; i++) {
  const reviewPrompt = `
Perform a detailed code review for this diff chunk.

Context summary:
${plan}

Focus:
- Logical or correctness errors
- Security and performance issues
- Code style, readability, maintainability
- Suggest concrete improvements and example fixes

Diff chunk ${i + 1}:
${chunks[i]}
`;

  try {
    console.log(`Reviewing chunk ${i + 1}/${chunks.length}...`);
    const analysis = await callLLM(MODEL_REVIEW, [
      { role: "system", content: "You are a rigorous code reviewer. Be direct and concise." },
      { role: "user", content: reviewPrompt },
    ], 1500, 0.1);
    reviews.push(`### Chunk ${i + 1}\n${analysis}`);
  } catch (err) {
    reviews.push(`### Chunk ${i + 1}\nError: ${err.message}`);
  }
}

// --- Stage 3: Synthesize Results ---
const synthesisPrompt = `
Merge and deduplicate the following chunk reviews into a single coherent PR review.

Include sections:
- Summary
- Key Findings
- Risks
- Suggested Improvements
- Tests to Add
Keep it concise and actionable.
`;

console.log("Synthesizing final review...");
const finalReview = await callLLM(MODEL_SUMMARY, [
  { role: "system", content: "You merge multi-part reviews into clear summaries." },
  { role: "user", content: `${synthesisPrompt}\n\n${reviews.join("\n\n")}` },
], 1200, 0.15);

// --- Post Results to PR ---
console.log("Posting AI review comments...");
await octokit.issues.createComment({
  owner,
  repo,
  issue_number: prNumber,
  body: `🤖 **AI Summary (model: \`${MODEL_SUMMARY}\`)**\n\n${plan}`,
});
await octokit.issues.createComment({
  owner,
  repo,
  issue_number: prNumber,
  body: `🤖 **AI Review (model: \`${MODEL_REVIEW}\`)**\n\n${finalReview}`,
});

console.log("✅ AI PR review complete.");
