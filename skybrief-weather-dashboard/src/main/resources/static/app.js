"use strict";

const $ = sel => document.querySelector(sel);

// Icon keys come from the backend; [day, night]
const ICONS = {
    clear: ["☀️", "🌙"], partly: ["⛅", "☁️"], cloudy: ["☁️", "☁️"], fog: ["🌫️", "🌫️"],
    drizzle: ["🌦️", "🌧️"], rain: ["🌧️", "🌧️"], snow: ["❄️", "❄️"], storm: ["⛈️", "⛈️"]
};
const iconFor = (key, day) => (ICONS[key] || ["🌡️", "🌡️"])[day ? 0 : 1];

// ---- Small helpers --------------------------------------------------------------
const store = {
    get(key, fallback) {
        try {
            const raw = localStorage.getItem(key);
            return raw === null ? fallback : JSON.parse(raw);
        } catch (e) { return fallback; }
    },
    set(key, value) {
        try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* storage unavailable */ }
    }
};

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

const deg = n => Math.round(n) + "°";
const hourOf = iso => parseInt(iso.slice(11, 13), 10);
const fmtHour = iso => { const h = hourOf(iso); return (h % 12 || 12) + " " + (h < 12 ? "AM" : "PM"); };
const fmtTime = iso => {
    if (!iso || iso.length < 16) return "–";
    const h = hourOf(iso);
    return (h % 12 || 12) + ":" + iso.slice(14, 16) + " " + (h < 12 ? "AM" : "PM");
};
const weekday = (date, index) =>
    index === 0 ? "Today" : new Date(date + "T12:00:00").toLocaleDateString("en-US", { weekday: "short" });

// ---- State ----------------------------------------------------------------------
const savedRecent = store.get("recent", []);
const state = {
    units: store.get("units", "metric") === "imperial" ? "imperial" : "metric",
    recent: Array.isArray(savedRecent) ? savedRecent : [],
    lastQuery: store.get("last", { city: "Hyderabad" })
};
let requestId = 0;

// ---- Rendering ------------------------------------------------------------------
function render(d) {
    const c = d.current;
    const today = d.daily[0];

    document.body.dataset.theme = c.theme;
    document.body.dataset.day = c.day ? "day" : "night";
    document.title = deg(c.temperature) + " " + d.place.name + " | SkyBrief";

    $("#place").textContent = [d.place.name, d.place.region, d.place.country].filter(Boolean).join(", ");
    state.timezone = c.timezone;
    state.observed = fmtTime(c.time);
    updateClock();
    $("#hero-icon").textContent = iconFor(c.icon, c.day);
    $("#temp").textContent = deg(c.temperature);
    $("#label").textContent = c.label;
    $("#hilo").textContent = "H " + deg(today.max) + "  L " + deg(today.min);
    $("#feels").textContent = deg(c.feelsLike);

    $("#humidity").textContent = c.humidity + "%";
    $("#wind").textContent = Math.round(c.windSpeed) + " " + d.units.wind;
    $("#rain").textContent = today.precipChance + "%";
    $("#sunrise").textContent = fmtTime(today.sunrise);
    $("#sunset").textContent = fmtTime(today.sunset);

    const hourly = $("#hourly");
    hourly.replaceChildren();
    d.hourly.forEach((h, i) => {
        const li = el("li", "hour");
        const icon = el("span", "hour-icon", iconFor(h.icon, h.day));
        icon.setAttribute("aria-hidden", "true");
        li.append(
            el("span", "hour-time", i === 0 ? "Now" : fmtHour(h.time)),
            icon,
            el("span", "hour-temp", deg(h.temperature)),
            el("span", "hour-rain", h.precipChance >= 20 ? h.precipChance + "%" : "")
        );
        hourly.appendChild(li);
    });

    const daily = $("#daily");
    daily.replaceChildren();
    const weekMin = Math.min(...d.daily.map(x => x.min));
    const weekMax = Math.max(...d.daily.map(x => x.max));
    const span = Math.max(weekMax - weekMin, 1);
    d.daily.forEach((day, i) => {
        const li = el("li", "day");
        const icon = el("span", "day-icon", iconFor(day.icon, true));
        icon.setAttribute("aria-hidden", "true");
        icon.title = day.label;
        const range = el("span", "range");
        const fill = el("span", "range-fill");
        fill.style.left = ((day.min - weekMin) / span * 100) + "%";
        fill.style.width = Math.max((day.max - day.min) / span * 100, 4) + "%";
        range.appendChild(fill);
        li.append(
            el("span", "day-name", weekday(day.date, i)),
            icon,
            el("span", "day-rain", day.precipChance >= 20 ? day.precipChance + "%" : ""),
            el("span", "day-min", deg(day.min)),
            range,
            el("span", "day-max", deg(day.max))
        );
        daily.appendChild(li);
    });
}

// Live local time for the searched place; the provider's reading itself updates every ~15 minutes
function updateClock() {
    if (!state.observed) return;
    let now = "";
    try {
        now = new Date().toLocaleTimeString("en-US", { timeZone: state.timezone || undefined, hour: "numeric", minute: "2-digit" });
    } catch (e) { /* unknown timezone: show only the update time */ }
    $("#updated").textContent = (now ? "Local time " + now + " · " : "") + "Updated " + state.observed;
}

function renderRecent() {
    const box = $("#recent");
    box.replaceChildren();
    state.recent.forEach(name => {
        const chip = el("button", "chip", name);
        chip.type = "button";
        chip.addEventListener("click", () => loadWeather({ city: name }));
        box.appendChild(chip);
    });
}

function renderUnits() {
    document.querySelectorAll(".units button").forEach(b =>
        b.classList.toggle("active", b.dataset.units === state.units));
}

function setLoading(on) {
    $("#main").classList.toggle("loading", on);
    document.querySelectorAll("#search button").forEach(b => { b.disabled = on; });
}

function showError(message) {
    const box = $("#error");
    box.textContent = message;
    box.hidden = false;
}

function hideError() {
    $("#error").hidden = true;
}

// ---- Data loading ---------------------------------------------------------------
async function loadWeather(query) {
    const id = ++requestId;
    state.lastQuery = query;
    setLoading(true);
    hideError();
    try {
        const params = new URLSearchParams(Object.assign({}, query, { units: state.units }));
        const res = await fetch("/api/weather?" + params.toString());
        const data = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(data.error || "Something went wrong. Please try again.");
        if (id !== requestId) return;          // a newer request replaced this one
        render(data);
        if (query.city) {
            state.recent = [data.place.name].concat(state.recent.filter(n => n !== data.place.name)).slice(0, 5);
            store.set("recent", state.recent);
            store.set("last", { city: data.place.name });
            renderRecent();
        }
    } catch (err) {
        if (id === requestId) showError(err.message);
    } finally {
        if (id === requestId) setLoading(false);
    }
}

// ---- Events ---------------------------------------------------------------------
$("#search").addEventListener("submit", e => {
    e.preventDefault();
    const city = $("#city").value.trim();
    if (city) loadWeather({ city });
});

$("#locate").addEventListener("click", () => {
    if (!navigator.geolocation) {
        showError("Your browser doesn't support location.");
        return;
    }
    navigator.geolocation.getCurrentPosition(
        pos => loadWeather({ lat: pos.coords.latitude.toFixed(4), lon: pos.coords.longitude.toFixed(4) }),
        () => showError("Location permission was denied. Search for a city instead.")
    );
});

document.querySelectorAll(".units button").forEach(button =>
    button.addEventListener("click", () => {
        if (state.units === button.dataset.units) return;
        state.units = button.dataset.units;
        store.set("units", state.units);
        renderUnits();
        loadWeather(state.lastQuery);
    }));

// ---- Start ----------------------------------------------------------------------
renderUnits();
renderRecent();
setInterval(updateClock, 20000);
loadWeather(state.lastQuery.city ? { city: state.lastQuery.city } : { city: "Hyderabad" });
