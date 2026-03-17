(function () {
    if (window.uxNotify && window.uxConfirm) return;

    var style = document.createElement('style');
    style.textContent = [
        '.ux-toast-container{position:fixed;top:1rem;right:1rem;z-index:99999;display:flex;flex-direction:column;gap:.5rem;max-width:360px}',
        '.ux-toast{padding:.75rem 1rem;border-radius:.75rem;color:#fff;font:600 14px/1.4 Inter,system-ui,sans-serif;box-shadow:0 10px 30px rgba(0,0,0,.2);animation:uxSlideIn .2s ease-out}',
        '.ux-toast.info{background:#334155}',
        '.ux-toast.success{background:#059669}',
        '.ux-toast.error{background:#dc2626}',
        '.ux-confirm-backdrop{position:fixed;inset:0;background:rgba(2,6,23,.55);display:none;align-items:center;justify-content:center;z-index:100000;padding:1rem}',
        '.ux-confirm{width:min(480px,100%);background:#fff;border-radius:1rem;box-shadow:0 20px 40px rgba(0,0,0,.25);padding:1rem 1rem .875rem}',
        '.ux-confirm-title{font:800 1rem/1.2 Inter,system-ui,sans-serif;color:#0f172a;margin:0 0 .5rem}',
        '.ux-confirm-message{font:500 .9rem/1.5 Inter,system-ui,sans-serif;color:#334155;white-space:pre-wrap;margin:0}',
        '.ux-confirm-actions{display:flex;justify-content:flex-end;gap:.5rem;margin-top:1rem}',
        '.ux-btn{border:none;border-radius:.625rem;padding:.55rem .9rem;font:700 .85rem/1 Inter,system-ui,sans-serif;cursor:pointer}',
        '.ux-btn.cancel{background:#e2e8f0;color:#0f172a}',
        '.ux-btn.ok{background:#4f46e5;color:#fff}',
        '@keyframes uxSlideIn{from{transform:translateY(-8px);opacity:0}to{transform:translateY(0);opacity:1}}'
    ].join('');
    document.head.appendChild(style);

    var toastContainer = document.createElement('div');
    toastContainer.className = 'ux-toast-container';
    document.body.appendChild(toastContainer);

    window.uxNotify = function (message, type, ms) {
        var toast = document.createElement('div');
        toast.className = 'ux-toast ' + (type || 'info');
        toast.textContent = String(message || '');
        toastContainer.appendChild(toast);
        setTimeout(function () {
            toast.remove();
        }, ms || 3000);
    };

    var backdrop = document.createElement('div');
    backdrop.className = 'ux-confirm-backdrop';
    backdrop.innerHTML = [
        '<div class="ux-confirm" role="dialog" aria-modal="true" aria-label="Confirmation">',
        '<h3 class="ux-confirm-title">Please Confirm</h3>',
        '<p class="ux-confirm-message"></p>',
        '<div class="ux-confirm-actions">',
        '<button type="button" class="ux-btn cancel">Cancel</button>',
        '<button type="button" class="ux-btn ok">Continue</button>',
        '</div>',
        '</div>'
    ].join('');
    document.body.appendChild(backdrop);

    var msgEl = backdrop.querySelector('.ux-confirm-message');
    var cancelBtn = backdrop.querySelector('.ux-btn.cancel');
    var okBtn = backdrop.querySelector('.ux-btn.ok');

    window.uxConfirm = function (message, opts) {
        opts = opts || {};
        backdrop.querySelector('.ux-confirm-title').textContent = opts.title || 'Please Confirm';
        cancelBtn.textContent = opts.cancelText || 'Cancel';
        okBtn.textContent = opts.okText || 'Continue';
        msgEl.textContent = String(message || '');
        backdrop.style.display = 'flex';

        return new Promise(function (resolve) {
            function cleanup(result) {
                backdrop.style.display = 'none';
                cancelBtn.removeEventListener('click', onCancel);
                okBtn.removeEventListener('click', onOk);
                backdrop.removeEventListener('click', onBackdrop);
                resolve(result);
            }
            function onCancel() { cleanup(false); }
            function onOk() { cleanup(true); }
            function onBackdrop(e) {
                if (e.target === backdrop) cleanup(false);
            }
            cancelBtn.addEventListener('click', onCancel);
            okBtn.addEventListener('click', onOk);
            backdrop.addEventListener('click', onBackdrop);
        });
    };
})();
