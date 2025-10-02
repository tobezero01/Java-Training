// ==== Utils ====
function customContext(e) {
    e.preventDefault();
    const card = document.getElementById('hoverCard');
    ctx.style.setProperty('--x', (e.offsetX + 8) + 'px');
    ctx.style.setProperty('--y', (e.offsetY + 8) + 'px');
    ctx.classList.add('show');
    log('contextmenu: mở menu custom', e);
    // ẩn menu khi click nơi khác
    window.addEventListener('click', () => ctx.classList.remove('show'), { once: true });
    return false; // chặn menu mặc định
}
window.customContext = customContext; // dùng cho inline handler


// ==== 6) Drag & Drop ====
const dragItem = document.getElementById('dragItem');
const dropZone = document.getElementById('dropZone');
dragItem.addEventListener('dragstart', (e) => { e.dataTransfer.setData('text/plain', 'drag-demo'); log('dragstart', e); });
dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.style.background = '#ecfeff'; });
dropZone.addEventListener('dragleave', () => { dropZone.style.background = ''; });
dropZone.addEventListener('drop', (e) => { e.preventDefault(); dropZone.style.background = ''; log('drop: nhận dữ liệu = ' + e.dataTransfer.getData('text/plain'), e); });


// ==== 7) Submit form ====
document.getElementById('demoForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const obj = Object.fromEntries(fd.entries());
    log('submit: dữ liệu form → ' + JSON.stringify(obj));
});


// ==== 8) Scroll / Resize ====
const scrollYEl = document.getElementById('scrollY');
const whEl = document.getElementById('wh');
function updateWH() { whEl.textContent = `${window.innerWidth}×${window.innerHeight}`; }
updateWH();
let ticking = false;
window.addEventListener('scroll', () => {
    if (!ticking) {
        window.requestAnimationFrame(() => {
            scrollYEl.textContent = Math.round(window.scrollY);
            ticking = false;
        });
        ticking = true;
    }
}, { passive: true });
window.addEventListener('resize', updateWH);


// ==== 9) Delegation ====
document.getElementById('list').addEventListener('click', (e) => {
    const li = e.target.closest('li');
    if (!li) return;
    log('delegation: click mục #' + li.dataset.id, e);
});
// Cho phép thêm mục động để kiểm thử delegation
(function () {
    const btn = document.getElementById('addItem');
    if (!btn) return;
    let nextId = 4;
    btn.addEventListener('click', () => {
        const ul = document.getElementById('list');
        const li = document.createElement('li');
        li.dataset.id = String(nextId++);
        li.textContent = 'Mục ' + li.dataset.id;
        ul.appendChild(li);
        log('delegation: đã thêm mục #' + li.dataset.id);
    });
})();