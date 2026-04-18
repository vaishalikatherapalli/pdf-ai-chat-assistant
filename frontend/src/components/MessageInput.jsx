import { useState } from 'react';

export default function MessageInput({ onSend, disabled }) {
    const [input, setInput] = useState('');

    function handleSubmit(e) {
        e.preventDefault();
        const trimmed = input.trim();
        if (!trimmed || disabled) return;
        onSend(trimmed);
        setInput('');
    }

    function handleKeyDown(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            handleSubmit(e);
        }
    }

    return (
        <form className="message-input-form" onSubmit={handleSubmit}>
            <textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Ask something about your PDFs..."
                rows={2}
                disabled={disabled}
            />
            <button type="submit" disabled={disabled || !input.trim()}>
                {disabled ? '...' : 'Send'}
            </button>
        </form>
    );
}
