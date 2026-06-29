import json, os, urllib.request, sys

current_design = os.environ.get("CURRENT_DESIGN", "")
feedback       = os.environ.get("FEEDBACK", "")
ticket_id      = os.environ.get("TICKET_ID", "")
gh_token       = os.environ.get("GH_TOKEN", "")

prompt = "\n\n".join([
    "You are a senior Java Spring Boot architect. You wrote the following technical design document and have received revision feedback from a human reviewer.",
    "## Current Design Document\n" + current_design,
    "## Reviewer Feedback\n" + feedback,
    "## Instructions\nRevise the design document to address the feedback. Keep all sections intact and only modify what the feedback requires. Output the complete revised Markdown document only — no preamble, no explanation.",
])

payload = {
    "model": "gpt-4o",
    "max_tokens": 8000,
    "messages": [{"role": "user", "content": prompt}]
}

req = urllib.request.Request(
    "https://models.inference.ai.azure.com/chat/completions",
    data=json.dumps(payload).encode(),
    headers={"Authorization": "Bearer " + gh_token, "Content-Type": "application/json"}
)

with urllib.request.urlopen(req) as resp:
    data = json.load(resp)

text = data["choices"][0]["message"]["content"]
design_path = "docs/design/" + ticket_id + ".md"
with open(design_path, "w") as f:
    f.write(text)
print("Revised design doc written to " + design_path)
