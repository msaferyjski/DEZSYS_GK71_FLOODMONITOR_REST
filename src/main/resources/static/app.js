const API = '/api/v1';
const REFRESH_MS = 10000;
let timer = null;

const MEASUREMENT_FIELDS = ['stationId', 'timestamp', 'waterLevel', 'flowRate', 'rainfall', 'temperature',
    'batteryLevel', 'status', 'warningLevel'];

function format() {
    return $('#format').val();
}

function request(path, params) {
    const xml = format() === 'xml';
    return $.ajax({
        url: API + path,
        data: params,
        headers: {Accept: xml ? 'application/xml' : 'application/json'},
        dataType: xml ? 'xml' : 'json'
    });
}

function fromXml(element, fields) {
    const obj = {};
    fields.forEach(f => obj[f] = $(element).children(f).text());
    return obj;
}

function parseStations(data) {
    if (format() === 'json') {
        return data.stations;
    }
    return $(data).find('stations > station').map((i, el) => fromXml(el, ['id', 'name', 'river'])).get();
}

function parseMeasurement(data) {
    return format() === 'json' ? data : fromXml($(data).children('measurement'), MEASUREMENT_FIELDS);
}

function parseMeasurements(data) {
    if (format() === 'json') {
        return data.measurements;
    }
    return $(data).find('measurements > measurement').map((i, el) => fromXml(el, MEASUREMENT_FIELDS)).get();
}

function errorMessage(xhr) {
    if (xhr.status === 0) {
        return 'Server nicht erreichbar';
    }
    let message = xhr.statusText;
    try {
        message = JSON.parse(xhr.responseText).message;
    } catch (e) {
        try {
            message = $($.parseXML(xhr.responseText)).find('message').text() || message;
        } catch (ignored) {
        }
    }
    return 'Fehler ' + xhr.status + ': ' + message;
}

function setState(type, text) {
    $('#state').attr('class', 'state ' + type).text(text);
}

function badge(level) {
    return $('<span>').addClass('badge ' + level).text(level);
}

function formatTime(iso) {
    return new Date(iso).toLocaleString('de-AT');
}

function toIso(value) {
    return value ? new Date(value).toISOString() : undefined;
}

function stationName(id) {
    return $('#station option[value="' + id + '"]').text() || id;
}

function loadStations() {
    setState('loading', 'Lade Stationen ...');
    return request('/stations').done(data => {
        const select = $('#station');
        const selected = select.val();
        select.empty();
        parseStations(data).forEach(s => select.append($('<option>').val(s.id).text(s.id + ' ' + s.name + ' (' + s.river + ')')));
        if (selected) {
            select.val(selected);
        }
    }).fail(xhr => setState('error', errorMessage(xhr)));
}

function renderLatest(m) {
    const cards = [
        [m.waterLevel + ' cm', 'Wasserstand'],
        [m.flowRate + ' m³/s', 'Durchfluss'],
        [m.rainfall + ' mm/h', 'Niederschlag'],
        [m.temperature + ' °C', 'Temperatur'],
        [m.batteryLevel + ' %', 'Akku'],
        [m.status, 'Status'],
        [badge(m.warningLevel), 'Warnstufe'],
        [formatTime(m.timestamp), 'Zeitpunkt']
    ];
    $('#latest').empty().append(cards.map(([value, label]) =>
        $('<div class="card">').append($('<strong>').append(value), $('<span>').text(label))));
}

function renderHistory(list) {
    const body = $('#history').empty();
    if (list.length === 0) {
        body.append('<tr><td colspan="8">Keine Messwerte gefunden</td></tr>');
        return;
    }
    list.forEach(m => body.append($('<tr>').append(
        $('<td>').text(stationName(m.stationId)),
        $('<td>').text(formatTime(m.timestamp)),
        $('<td>').text(m.waterLevel),
        $('<td>').text(m.flowRate),
        $('<td>').text(m.rainfall),
        $('<td>').text(m.batteryLevel),
        $('<td>').text(m.status),
        $('<td>').append(badge(m.warningLevel))
    )));
}

function loadData() {
    const id = $('#station').val();
    if (!id) {
        return;
    }
    setState('loading', 'Lade Messwerte ...');
    const params = {
        from: toIso($('#from').val()),
        to: toIso($('#to').val()),
        limit: $('#limit').val() || undefined,
        warningLevel: $('#warningLevel').val() || undefined
    };
    const latest = request('/stations/' + id + '/measurements/latest')
        .done(data => renderLatest(parseMeasurement(data)));
    const history = request('/stations/' + id + '/measurements', params)
        .done(data => renderHistory(parseMeasurements(data)));
    $.when(latest, history)
        .done(() => setState('ok', 'Aktualisiert ' + new Date().toLocaleTimeString('de-AT') + ' (' + format().toUpperCase() + ')'))
        .fail(xhr => setState('error', errorMessage(xhr)));
}

function scheduleRefresh() {
    clearInterval(timer);
    if ($('#autoRefresh').is(':checked')) {
        timer = setInterval(loadData, REFRESH_MS);
    }
}

$(function () {
    $('#apply').on('click', loadData);
    $('#station, #warningLevel').on('change', loadData);
    $('#format').on('change', () => loadStations().done(loadData));
    $('#autoRefresh').on('change', scheduleRefresh);
    loadStations().done(loadData);
    scheduleRefresh();
});
