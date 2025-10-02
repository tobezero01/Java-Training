import { Book } from './Book.js';

export class BookManager {
    constructor(storage) {
        this.storage = storage;
        this.items = this.storage.load().map(Book.fromJSON);
        this._nextId = this.items.reduce((m, b) => Math.max(m, b.id), 0) + 1;
    }

    list(query = '') {
        const q = query.trim().toLowerCase();
        if (!q) return [...this.items];
        return this.items.filter(b =>
            b.title.toLowerCase().includes(q) || b.author.toLowerCase().includes(q)
        );
    }

    add({ title, author, year, isRead }) {
        const book = new Book({ id: this._nextId++, title, author, year, isRead });
        this.items.push(book);
        this._persist();
        return book;
    }

    remove(id) {
        const i = this.items.findIndex(b => b.id === id);
        if (i >= 0) {
            this.items.splice(i, 1);
            this._persist();
            return true;
        }
        return false;
    }

    toggle(id) {
        const book = this.items.find(b => b.id === id);
        if (book) {
            book.toggle();
            this._persist();
        }
        return book;
    }

    clear() {
        this.items = [];
        this._persist();
    }

    stats() {
        const total = this.items.length;
        const read = this.items.filter(b => b.isRead).length;
        return { total, read, unread: total - read };
    }

    _persist() { this.storage.save(this.items.map(b => b.toJSON())); }
}
