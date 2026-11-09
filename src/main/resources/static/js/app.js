// === Toast Notification ===
function showToast(message, type) {
    const toast = document.createElement('div');
    toast.className = 'toast toast-' + type;
    toast.textContent = message;
    document.body.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add('show'));
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// === Pipeline Run ===
function runPipeline() {
    const btn = document.getElementById('run-pipeline-btn');
    const statusEl = document.getElementById('pipeline-status');
    const resultEl = document.getElementById('pipeline-result');

    if (!btn) return;

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Running...';

    if (statusEl) {
        statusEl.innerHTML = '<span class="status-indicator status-running"><span class="pulse pulse-running"></span> Running</span>';
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    const headers = {};
    if (csrfToken && csrfHeader) {
        headers[csrfHeader.content] = csrfToken.content;
    }

    fetch('/api/pipeline/run', { method: 'POST', headers: headers, credentials: 'same-origin' })
        .then(res => res.json())
        .then(data => {
            btn.disabled = false;
            btn.innerHTML = 'Run Pipeline';

            if (statusEl) {
                statusEl.innerHTML = '<span class="status-indicator status-idle"><span class="pulse pulse-idle"></span> Idle</span>';
            }

            if (data.status === 'SUCCESS') {
                showToast('Pipeline completed! Sent ' + data.sent + ' articles.', 'success');
                if (resultEl) {
                    resultEl.innerHTML =
                        '<div class="card">' +
                        '<h3 class="section-title">Last Run Result</h3>' +
                        '<p>Collected: ' + data.collected + ' | Filtered: ' + data.filtered +
                        ' | Evaluated: ' + data.evaluated + ' | Sent: ' + data.sent + '</p>' +
                        '</div>';
                }
                setTimeout(() => location.reload(), 2000);
            } else if (data.status === 'SKIPPED') {
                showToast('Pipeline is already running.', 'error');
            } else {
                showToast('Pipeline failed: ' + (data.error || 'Unknown error'), 'error');
            }
        })
        .catch(err => {
            btn.disabled = false;
            btn.innerHTML = 'Run Pipeline';
            showToast('Request failed: ' + err.message, 'error');
        });
}

// === Score color helper ===
function getScoreClass(score) {
    if (score >= 7) return 'score-high';
    if (score >= 4) return 'score-mid';
    return 'score-low';
}
