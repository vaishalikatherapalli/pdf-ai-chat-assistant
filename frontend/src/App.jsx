import { useState } from 'react';
import ChatBox from './components/ChatBox';
import MessageInput from './components/MessageInput';
import PdfUpload from './components/PdfUpload';
import { streamChat } from './services/api';
import './App.css';

export default function App() {
    const [messages, setMessages] = useState([]);
    const [streaming, setStreaming] = useState(false);

    async function handleSend(userMessage) {
        const userMsg = { role: 'user', content: userMessage };
        const assistantMsg = { role: 'assistant', content: '', streaming: true };

        setMessages((prev) => [...prev, userMsg, assistantMsg]);
        setStreaming(true);

        await streamChat(
            userMessage,
            (token) => {
                setMessages((prev) => {
                    const updated = [...prev];
                    const last = updated[updated.length - 1];
                    updated[updated.length - 1] = { ...last, content: last.content + token };
                    return updated;
                });
            },
            () => {
                setMessages((prev) => {
                    const updated = [...prev];
                    updated[updated.length - 1] = {
                        ...updated[updated.length - 1],
                        streaming: false,
                    };
                    return updated;
                });
                setStreaming(false);
            },
            (err) => {
                setMessages((prev) => {
                    const updated = [...prev];
                    updated[updated.length - 1] = {
                        role: 'assistant',
                        content: 'Error: ' + err,
                        streaming: false,
                    };
                    return updated;
                });
                setStreaming(false);
            }
        );
    }

    return (
        <div className="app">
            <header className="app-header">
                <h1>AI Developer Assistant</h1>
            </header>
            <div className="app-body">
                <aside className="sidebar">
                    <PdfUpload />
                </aside>
                <main className="chat-area">
                    <ChatBox messages={messages} />
                    <MessageInput onSend={handleSend} disabled={streaming} />
                </main>
            </div>
        </div>
    );
}
