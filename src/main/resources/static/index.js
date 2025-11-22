const apiBase = "http://localhost:5500";

async function api(method, path, body) {
  const opts = {
    method,
    headers: { "Content-Type": "application/json" },
  };

  if (body !== undefined) {
    opts.body = JSON.stringify(body);
  }

  const res = await fetch(apiBase + path, opts);

  // 성공
  if (res.ok) {
    return res.json().catch(() => null);
  }

  // 실패 → JSON 먼저 시도
  let errorMessage = "알 수 없는 오류가 발생했습니다.";

  try {
    const err = await res.json();
    if (err.error) errorMessage = err.error;
  } catch (_) {
    const text = await res.text();
    if (text) errorMessage = text;
  }

  throw new Error(errorMessage);
}


async function createLocker() {
  const totalInput = document.getElementById("create-total");
  const totalVal = totalInput.value ? parseInt(totalInput.value, 10) : null;

  const body = totalVal ? { umbrellaTotal: totalVal } : {};
  try {
    const data = await api("POST", "/api/lockers", body);
    appendLog(`[CREATE] locker created: ${JSON.stringify(data)}`);
    totalInput.value = "";
    loadLockers();
  } catch (e) {
    alert("생성 실패: " + e.message);
  }
}

async function loadLockers() {
  try {
    const data = await api("GET", "/api/lockers");
    renderLockers(data);
  } catch (e) {
    alert("조회 실패: " + e.message);
  }
}

function getSelectedId() {
  const idStr = document.getElementById("locker-id").value;
  const id = parseInt(idStr, 10);
  if (!id) {
   alert("먼저 락커 ID를 입력해주세요.");
   throw new Error("no id");
  }
  return id;
}

async function rentLocker() {
  try {
    const id = getSelectedId();
    const data = await api("POST", `/api/lockers/${id}/rent`);
    appendLog(`[RENT] ${id}: ${JSON.stringify(data)}`);
  } catch (e) {
    if (e.message !== "no id") alert("대여 실패: " + e.message);
  }
}

async function returnLocker() {
  try {
    const id = getSelectedId();
    const data = await api("POST", `/api/lockers/${id}/return`);
    appendLog(`[RETURN] ${id}: ${JSON.stringify(data)}`);
  } catch (e) {
    if (e.message !== "no id") alert("반납 실패: " + e.message);
  }
}

async function brokenLocker() {
  try {
    const id = getSelectedId();
    const data = await api("POST", `/api/lockers/${id}/broken`);
    appendLog(`[BROKEN] ${id}: ${JSON.stringify(data)}`);
  } catch (e) {
  if (e.message !== "no id") alert("고장 처리 실패: " + e.message);
  }
}

async function repairLocker() {
  try {
    const id = getSelectedId();
    const restStr = document.getElementById("repair-rest").value;
    if (restStr === "") {
      alert("수리 후 남은 우산 개수를 입력해주세요.");
      return;
    }
    const rest = parseInt(restStr, 10);
    const data = await api("POST", `/api/lockers/${id}/repair`, { umbrellaRest: rest });
    appendLog(`[REPAIR] ${id}: ${JSON.stringify(data)}`);
  } catch (e) {
    if (e.message !== "no id") alert("수리 실패: " + e.message);
  }
}

async function deleteLocker() {
  try {
    const id = getSelectedId();
    if (!confirm(`정말로 락커 #${id} 를 삭제하시겠습니까?`)) {
      return;
    }
    const res = await fetch(`/api/lockers/${id}`, { method: "DELETE" });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`HTTP ${res.status}: ${text}`);
    }
    appendLog(`[DELETE] ${id}: deleted`);
  } catch (e) {
    if (e.message !== "no id") alert("삭제 실패: " + e.message);
  }
}

function renderLockers(lockers) {
  const container = document.getElementById("lockers-table");
  if (!lockers || lockers.length === 0) {
    container.innerHTML = "<p>등록된 우산함이 없습니다.</p>";
     return;
  }
  let html = "<table><thead><tr>" +
    "<th>ID</th><th>Status</th><th>Rest</th><th>Total</th><th>UpdatedAt</th>" +
    "</tr></thead><tbody>";
  lockers.forEach(l => {
    html += `<tr>
      <td>${l.id}</td>
      <td>${l.status}</td>
      <td>${l.umbrellaRest}</td>
      <td>${l.umbrellaTotal}</td>
      <td>${l.updatedAt}</td>
    </tr>`;
  });
  html += "</tbody></table>";
  container.innerHTML = html;
}

function appendLog(line) {
  const log = document.getElementById("log");
  const now = new Date().toISOString();
  log.textContent = `[${now}] ${line}\n` + log.textContent;
}

function setupSSE() {
  const es = new EventSource("/stream/lockers");

  es.addEventListener("locker_change", (e) => {
    try {
      const payload = JSON.parse(e.data);
      appendLog("[SSE] " + JSON.stringify(payload));
      loadLockers();
    } catch (err) {
      appendLog("[SSE raw] " + e.data);
    }
  });

  es.onopen = () => appendLog("SSE 연결 성공");
  es.onerror = (e) => {
    appendLog("SSE 에러 발생 (콘솔 확인)");
    console.error("SSE error", e);
  };
}

// 페이지가 로드되면 초기 데이터 + SSE 연결
window.addEventListener("load", () => {
  loadLockers();
  setupSSE();
});