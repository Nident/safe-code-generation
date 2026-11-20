FROM node:22-bookworm-slim
ARG CODEX_VERSION
RUN test -n "$CODEX_VERSION" && npm install --global "@openai/codex@${CODEX_VERSION}"
USER node
WORKDIR /work
ENV CODEX_HOME=/home/node/.codex
ENTRYPOINT ["codex"]
