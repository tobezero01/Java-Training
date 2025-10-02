export class Storage {
    constructor(key) { this.key = key; }
    load() {
        try {
            const raw = localStorage.getItem(this.key);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            console.error('Storage.load error', e);
            return [];
        }
    }
    save(arr) {
        try {
            localStorage.setItem(this.key, JSON.stringify(arr));
        } catch (e) {
            console.error('Storage.save error', e);
        }
    }
}
