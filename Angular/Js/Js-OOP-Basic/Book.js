export class Book {
    constructor({ id, title, author, year = null, isRead = false }) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year ? Number(year) : null;
        this.isRead = Boolean(isRead);
    }
    toggle() { this.isRead = !this.isRead; }
    toJSON() {
        return {
            id: this.id, title: this.title,
            author: this.author, year: this.year,
            isRead: this.isRead
        };
    }
    static fromJSON(obj) { return new Book(obj); }
}
