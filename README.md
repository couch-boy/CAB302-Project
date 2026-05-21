# [Javadocs project documentation](https://couch-boy.github.io/CAB302-Project/)

## Ollama Setup

This project uses Ollama as a local LLM runtime.

1. Install Ollama from the official website.
2. Start Ollama.
3. Pull the required model:

   ```bash
   ollama pull llama3.2
   ```

4. Make sure Ollama is running on:

   ```text
   http://localhost:11434
   ```

5. Run the Java application.

Important:
- Do not commit Ollama model files to the repository.
- Each user must install Ollama and pull the required model on their own computer.
- The project uses Ollama4j to communicate with the local Ollama server.
- Optional configuration values are `OLLAMA_BASE_URL` and `OLLAMA_MODEL`; defaults are shown in `.env.example`.
