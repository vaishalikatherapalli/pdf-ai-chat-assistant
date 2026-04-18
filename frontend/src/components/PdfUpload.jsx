import { useState, useEffect } from 'react';
import { uploadPdf, listPdfs, deletePdf } from '../services/api';

export default function PdfUpload() {
    const [files, setFiles] = useState([]);
    const [uploading, setUploading] = useState(false);
    const [status, setStatus] = useState('');

    useEffect(() => { fetchFiles(); }, []);

    async function fetchFiles() {
        const list = await listPdfs();
        setFiles(list);
    }

    async function handleUpload(e) {
        const file = e.target.files[0];
        if (!file) return;
        setUploading(true);
        setStatus('');
        await uploadPdf(
            file,
            (msg) => { setStatus(msg); fetchFiles(); },
            (err) => setStatus('Error: ' + err)
        );
        setUploading(false);
        e.target.value = '';
    }

    async function handleDelete(filename) {
        await deletePdf(filename);
        fetchFiles();
    }

    return (
        <div className="pdf-panel">
            <h3>PDF Documents</h3>
            <label className="upload-btn">
                {uploading ? 'Uploading...' : 'Upload PDF'}
                <input type="file" accept=".pdf" onChange={handleUpload} hidden />
            </label>
            {status && <p className="upload-status">{status}</p>}
            <ul className="file-list">
                {files.map((f) => (
                    <li key={f}>
                        <span>{f}</span>
                        <button onClick={() => handleDelete(f)}>✕</button>
                    </li>
                ))}
                {files.length === 0 && <li className="no-files">No PDFs uploaded yet</li>}
            </ul>
        </div>
    );
}
