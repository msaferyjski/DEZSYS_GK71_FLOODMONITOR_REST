const el = id => document.getElementById(id);
let key = '';
let controller;
let page = 1;
let snapshot;
let totalPages = 0;

function fields(node) {
    return Object.fromEntries(Array.from(node.children, child => [child.tagName, child.textContent]));
}

async function request(path, params, signal, xml) {
    const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== ''));
    const response = await fetch('/api/v1' + path + '?' + query, {
        headers: {'X-API-Key': key, Accept: xml ? 'application/xml' : 'application/json'}, signal
    });
    const raw = await response.text();
    const data = xml ? new DOMParser().parseFromString(raw, 'application/xml') : JSON.parse(raw);
    if (!response.ok) {
        const message = xml ? data.querySelector('message')?.textContent : data.message;
        throw Object.assign(new Error(message || response.statusText), {status: response.status});
    }
    return data;
}

function textElement(tag, value, className) {
    const node = document.createElement(tag);
    node.textContent = value;
    if (className) node.className = className;
    return node;
}

function time(value) {
    return new Date(value).toLocaleString('de-AT');
}

function renderLatest(m) {
    if (!m) {
        el('latest').textContent = 'Noch kein Messwert empfangen.';
        return;
    }
    const values = [
        ['Wasserstand', m.waterLevel + ' cm'], ['Durchfluss', m.flowRate + ' m³/s'],
        ['Niederschlag', m.rainfall + ' mm/h'], ['Temperatur', m.temperature + ' °C'],
        ['Akku', m.batteryLevel + ' %'], ['Status', m.status], ['Warnstufe', m.warningLevel],
        ['Zeitpunkt', time(m.timestamp)]
    ];
    el('latest').replaceChildren(...values.map(([label, value]) => {
        const card = document.createElement('div');
        card.className = 'card';
        card.append(textElement('span', label), textElement('strong', value, label === 'Warnstufe' ? value : ''));
        return card;
    }));
}

function renderHistory(data, xml) {
    const meta = xml ? fields(data.documentElement) : data;
    const list = xml ? Array.from(data.querySelectorAll('measurements > measurement'), fields) : data.measurements;
    const rows = list.map(m => {
        const row = document.createElement('tr');
        const values = [m.stationId, time(m.timestamp), m.waterLevel, m.flowRate, m.rainfall, m.batteryLevel, m.status];
        row.append(...values.map(value => textElement('td', value)), textElement('td', m.warningLevel, m.warningLevel));
        return row;
    });
    if (!list.length) {
        const cell = textElement('td', 'Keine Messwerte für diese Auswahl.');
        cell.colSpan = 8;
        const row = document.createElement('tr');
        row.append(cell);
        rows.push(row);
    }
    el('history').replaceChildren(...rows);
    totalPages = Number(meta.totalPages);
    el('pageInfo').textContent = totalPages ? 'Seite ' + meta.page + ' von ' + totalPages + ' · ' + meta.total + ' Messwerte' : '0 Messwerte';
    el('prev').disabled = page <= 1;
    el('next').disabled = page >= totalPages;
}

function clearData() {
    controller?.abort();
    el('latest').replaceChildren();
    el('history').replaceChildren();
    el('pageInfo').textContent = '';
    el('prev').disabled = el('next').disabled = true;
}

function session(active) {
    el('keyLabel').hidden = el('login').hidden = active;
    el('key').disabled = active;
    el('logout').hidden = !active;
}

async function load(reloadStations = false) {
    if (!key || !el('filters').reportValidity()) return;
    controller?.abort();
    controller = new AbortController();
    const signal = controller.signal;
    const xml = el('format').value === 'xml';
    el('state').textContent = 'Lade Messwerte …';
    el('prev').disabled = el('next').disabled = true;
    try {
        if (reloadStations || !el('station').options.length) {
            const data = await request('/stations', {}, signal, xml);
            const stations = xml ? Array.from(data.querySelectorAll('stations > station'), fields) : data.stations;
            const selected = el('station').value;
            el('station').replaceChildren(...stations.map(s => new Option(s.name + ' (' + s.river + ')', s.id)));
            if (stations.some(s => s.id === selected)) el('station').value = selected;
            session(true);
        }
        if (!el('station').value) {
            clearData();
            el('state').textContent = 'Keine Station vorhanden.';
            return;
        }
        const path = '/stations/' + encodeURIComponent(el('station').value) + '/measurements';
        let latest;
        try {
            const data = await request(path + '/latest', {}, signal, xml);
            latest = xml ? fields(data.documentElement) : data;
        } catch (error) {
            if (error.status !== 404) throw error;
        }
        const from = el('from').value;
        const to = el('to').value;
        if (page === 1) snapshot = latest?.timestamp;
        const upper = to ? new Date(to).toISOString() : '';
        const start = from ? new Date(from).toISOString() : '';
        const useSnapshot = snapshot && (!start || Date.parse(start) <= Date.parse(snapshot))
            && (!upper || Date.parse(snapshot) < Date.parse(upper));
        const data = await request(path, {
            from: start, to: useSnapshot ? snapshot : upper,
            limit: el('limit').value, warningLevel: el('warningLevel').value, page, sort: el('sort').value
        }, signal, xml);
        renderLatest(latest);
        renderHistory(data, xml);
        el('state').textContent = 'Aktualisiert um ' + new Date().toLocaleTimeString('de-AT') + ' · ' + el('format').value.toUpperCase();
    } catch (error) {
        if (signal.aborted) return;
        clearData();
        if (error.status === 401) logout();
        el('state').textContent = (error.status ? 'HTTP ' + error.status + ' · ' : '') + error.message;
    }
}

function restart() {
    page = 1;
    snapshot = undefined;
    load();
}

function logout() {
    key = '';
    session(false);
    clearData();
    el('station').replaceChildren();
    el('state').textContent = 'Zum Abrufen der Daten anmelden.';
    el('key').focus();
}

el('access').onsubmit = event => {
    event.preventDefault();
    key = el('key').value;
    el('key').value = '';
    page = 1;
    load(true);
};
el('logout').onclick = logout;
el('filters').onsubmit = event => {
    event.preventDefault();
    restart();
};
for (const id of ['station', 'from', 'to', 'warningLevel', 'limit', 'sort', 'format']) {
    el(id).onchange = restart;
}
el('prev').onclick = () => { page--; load(); };
el('next').onclick = () => { page++; load(); };
setInterval(() => {
    if (el('autoRefresh').checked && page === 1 && key) load();
}, 10000);
