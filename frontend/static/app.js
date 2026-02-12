const searchInput = document.getElementById("searchInput");
const autocompleteBox = document.getElementById("autocomplete");
const resultsBox = document.getElementById("results");
const latencyBox = document.getElementById("latency");
const homeInfo = document.getElementById("homeInfo");

const docViewer = document.getElementById("docViewer");
const docText = document.getElementById("docText");
const closeDoc = document.getElementById("closeDoc");

let debounceTimer = null;

/* =========================
   Autocomplete
   ========================= */

searchInput.addEventListener("input", () => {
    const value = searchInput.value.trim();

    if (value === "") {
        autocompleteBox.innerHTML = "";
        resultsBox.innerHTML = "";
        homeInfo.style.display = "block";
        return;
    }

    clearTimeout(debounceTimer);

    debounceTimer = setTimeout(() => {
        fetch(`/api/autocomplete?prefix=${encodeURIComponent(value)}`)
            .then(res => res.json())
            .then(data => {
                autocompleteBox.innerHTML = "";

                data.suggestions.forEach(s => {
                    const div = document.createElement("div");
                    div.textContent = s;
                    div.onclick = () => {
                        searchInput.value = s;
                        autocompleteBox.innerHTML = "";
                        runSearch();
                    };
                    autocompleteBox.appendChild(div);
                });
            });
    }, 150);
});

/* =========================
   Search
   ========================= */

searchInput.addEventListener("keydown", e => {
    if (e.key === "Enter") {
        autocompleteBox.innerHTML = "";
        runSearch();
    }
});

function runSearch() {
    const query = searchInput.value.trim();
    if (!query) return;

    fetch(`/api/search?q=${encodeURIComponent(query)}`)
        .then(res => res.json())
        .then(data => {
            latencyBox.textContent = `Search latency: ${data.latencyMs} ms`;
            renderResults(data.results);
        });
}

function renderResults(results) {
    resultsBox.innerHTML = "";

    if (!results || results.length === 0) {
        homeInfo.style.display = "block";
        return;
    }

    homeInfo.style.display = "none";

    results.forEach(r => {
        const div = document.createElement("div");
        div.className = "result";

        div.innerHTML = `
            <div class="result-title">${escapeHtml(r.title)}</div>
            <div class="result-preview">${escapeHtml(r.preview)}</div>
        `;

        div.onclick = () => loadDocument(r.docId);
        resultsBox.appendChild(div);
    });
}

/* =========================
   Document Viewer
   ========================= */

function loadDocument(docId) {
    fetch(`/api/doc?id=${docId}`)
        .then(res => res.json())
        .then(data => {
            docText.innerHTML = formatDocument(data.text);
            docViewer.style.display = "block";
        });
}

closeDoc.onclick = () => {
    docViewer.style.display = "none";
};

/* =========================
   Formatting Helpers
   ========================= */

function formatDocument(text) {
    text = text.replace(/\r\n/g, "\n");

    const paragraphs = text.split(/\n{2,}/);

    return paragraphs
        .map(p => `<p>${escapeHtml(p.trim())}</p>`)
        .join("");
}

function escapeHtml(str) {
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}
