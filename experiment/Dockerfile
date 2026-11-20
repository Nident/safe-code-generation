FROM node:22-bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*
ARG CODEX_VERSION
RUN test -n "$CODEX_VERSION" && npm install --global "@openai/codex@${CODEX_VERSION}"
RUN mkdir -p /home/node/.codex && chown node:node /home/node/.codex
USER node
WORKDIR /work
ENV CODEX_HOME=/home/node/.codex
ENTRYPOINT ["codex"]
