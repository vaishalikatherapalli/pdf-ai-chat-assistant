# PDF AI Chat Assistant

A chat-based AI assistant that reads your uploaded PDF documents and answers questions about them — powered by a local LLM via Ollama. No cloud API keys required.

## What It Does

- Upload one or more PDF files via the sidebar
- Ask questions about the content in a chat interface
- Get streaming word-by-word responses from a local AI model
- All processing happens on your machine — your documents stay private

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + Vite |
| Backend | Spring Boot 3 (Java 21) |
| AI Model | Ollama (llama3.2) — runs locally |
| PDF Parsing | Apache PDFBox 3 |
| Retrieval | Keyword-based chunk matching (RAG) |
| Streaming | Server-Sent Events (SSE) |

## Architecture

```
React UI ──► Spring Boot API ──► Ollama (local LLM)
                   │
            PDF Upload & Parsing
         (chunk → keyword search → context)
```

When you ask a question:
1. Spring Boot finds the most relevant chunks from your uploaded PDFs
2. Those chunks + your question are sent to Ollama as context
3. Ollama streams the answer back word by word to the React UI

---

## Prerequisites

- Java 21+
- Node.js 18+
- [Ollama](https://ollama.com) installed and running

---

## Setup & Running

### 1. Install and Start Ollama

Download Ollama from [ollama.com](https://ollama.com), then:

```bash
ollama serve
ollama pull llama3.2
```

### 2. Run the Backend

```bash
cd backend
mvn spring-boot:run
```

> If you don't have Maven installed, download it from [maven.apache.org](https://maven.apache.org) or install via Homebrew: `brew install maven`

The backend starts on **http://localhost:8080**

### 3. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**

### 4. Use the App

1. Open **http://localhost:5173** in your browser
2. Upload a PDF using the sidebar
3. Type a question in the chat box and press Enter

---

## Configuration

Edit `backend/src/main/resources/application.properties` to change the model or port:

```properties
server.port=8080
ollama.api.url=http://localhost:11434
ollama.api.model=llama3.2   # or mistral, llama3.2:1b, etc.
```

Available Ollama models: `ollama list`

---

## Project Structure

```
pdf-ai-chat-assistant/
├── backend/                        # Spring Boot app
│   ├── pom.xml
│   └── src/main/java/com/aiassistant/
│       ├── controller/
│       │   ├── ChatController.java     # SSE streaming endpoint
│       │   └── PdfController.java     # PDF upload/list/delete
│       ├── service/
│       │   ├── ChatService.java        # Calls Ollama, streams response
│       │   └── PdfIngestionService.java # PDF parsing & chunk retrieval
│       ├── model/
│       │   └── ChatRequest.java
│       └── config/
│           └── CorsConfig.java
│
└── frontend/                       # React + Vite app
    ├── index.html
    └── src/
        ├── App.jsx
        ├── App.css
        ├── components/
        │   ├── ChatBox.jsx             # Message list
        │   ├── MessageInput.jsx        # Input bar
        │   └── PdfUpload.jsx          # Sidebar file manager
        └── services/
            └── api.js                 # Fetch + SSE stream parsing
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/chat` | Stream a chat response (SSE) |
| POST | `/api/pdf/upload` | Upload a PDF |
| GET | `/api/pdf/list` | List uploaded PDFs |
| DELETE | `/api/pdf/{filename}` | Remove a PDF |
| GET | `/api/health` | Health check |
