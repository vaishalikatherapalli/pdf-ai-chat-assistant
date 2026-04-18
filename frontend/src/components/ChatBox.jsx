import { useEffect, useRef } from 'react';

export default function ChatBox({ messages }) {
    const bottomRef = useRef(null);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    return (
        <div className="chat-box">
            {messages.length === 0 && (
                <div className="empty-state">
                    Upload a PDF and ask me anything about it.
                </div>
            )}
            {messages.map((msg, i) => (
                <div key={i} className={`message ${msg.role}`}>
                    <div className="message-bubble">
                        {msg.content}
                        {msg.streaming && <span className="cursor">▌</span>}
                    </div>
                </div>
            ))}
            <div ref={bottomRef} />
        </div>
    );
}
