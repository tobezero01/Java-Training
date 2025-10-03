// Elements
const form = document.getElementById('todoForm');
const input = document.getElementById('todoInput');
const list = document.getElementById('todoList');
const countEl = document.getElementById('pendingCount');
const clearAllBtn = document.getElementById('clearAll');
const errorMessage = document.getElementById('errorMessage');
const searchBtn = document.getElementById('searchBtn');
const noResults = document.getElementById('noResults');

// mảng đại diện chứa toàn bộ anh sách task
const getTaskNames = () => Array.from(list.querySelectorAll('.todo-text'))
    .map(el => el.textContent.trim().toLowerCase());


form.addEventListener('submit', (e) => {
    e.preventDefault();
    const ck = e.submitter?.id;
    if (ck === 'searchBtn') return handleSearch();
    if (ck === 'addBtn') return handleAdd();
});

function handleAdd() {
    const text = input.value.trim();
    if (!text) {
        addError('* Input required');
        return input.focus();
    }
    if (text.length < 4) {
        addError('* Task must be at least 4 characters');
        return input.focus();
    }
    if (hasDuplicate(text)) { addError('* Task name already exists'); return input.focus(); }

    clearError();
    addTodo(text);
    input.value = '';
    input.focus();
    Array.from(list.children).forEach(li => li.classList.remove('is-hidden'));
    setNoResults(false);
    updateCount();
}

function handleSearch() {
    const key = input.value.trim().toLowerCase();
    if (!key) {
        Array.from(list.children).forEach(li => li.classList.remove('is-hidden'));
        setNoResults(false);
        return;
    }

    let any = false;
    Array.from(list.querySelectorAll('.todo')).forEach(li => {
        const name = li.querySelector('.todo-text')?.textContent.trim().toLowerCase() || '';
        const match = name.includes(key);
        li.classList.toggle('is-hidden', !match);
        if (match) any = true;
    });
    setNoResults(!any);
}

list.addEventListener('click', (e) => {
    const delBtn = e.target.closest('.delete');
    if (!delBtn) return;
    const li = delBtn.closest('.todo');
    if (li) {
        li.remove();
        updateCount();

        const q = input.value.trim().toLowerCase();
        if (q) {
            const any = Array.from(list.querySelectorAll('.todo'))
                .some(li => !li.classList.contains('is-hidden'));
            setNoResults(!any);
        }
    }
});

clearAllBtn.addEventListener('click', () => {
    list.innerHTML = '';
    input.value = '';
    updateCount();
    clearError();
    setNoResults(false);
});



input.addEventListener('input', () => {
    if (input.value.trim()) clearError();
});

function hasDuplicate(name) {
    const lower = name.trim().toLowerCase();
    return getTaskNames().includes(lower);
}

function setNoResults(show) {
    noResults.style.display = show ? 'block' : 'none';
}

function updateCount() {
    const n = list.children.length;
    countEl.textContent = n;
    list.classList.toggle('scrolling', n >= 5);
}

function addTodo(text) {
    const html = `
    <li class="todo" draggable="true">
      <span class="todo-text">${text}</span>
      <button class="delete" title="Delete">
        X
      </button>
    </li>`;
    list.insertAdjacentHTML('beforeend', html);

}

function addError(msg) {
    errorMessage.textContent = msg;
    errorMessage.style.display = 'block';
}
function clearError() {
    errorMessage.textContent = '';
    errorMessage.style.display = 'none';
}


// ===== Drag & Drop Reorder =====
list.addEventListener('dragstart', (e) => {
    const li = e.target.closest('.todo');
    if (!li) return;
    li.classList.add('dragging');
})

list.addEventListener('dragend', (e) => {
    const li = e.target.closest('.todo');
    if (li) li.classList.remove('dragging');
});

list.addEventListener('dragover', (e) => {
    e.preventDefault();
    const dragging = list.querySelector('.dragging');
    if (!dragging) return;

    // Tìm phần tử đứng sau vị trí con trỏ để chèn trước nó
    const after = getDragAfterElement(list, e.clientY);
    if (after == null) {
        list.appendChild(dragging);
    } else {
        list.insertBefore(dragging, after);
    }
});

list.addEventListener('drop', (e) => e.preventDefault());

function getDragAfterElement(container, y) {
    const items = [...container.querySelectorAll('.todo:not(.dragging):not(.is-hidden)')];

    // Tính khoảng cách đến tâm từng item; chọn item có offset âm nhưng lớn nhất
    return items.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - (box.top + box.height / 2);
        if (offset < 0 && offset > closest.offset) {
            return { offset, element: child };
        }
        return closest;
    }, { offset: Number.NEGATIVE_INFINITY, element: null }).element;
}