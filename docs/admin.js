(() => {
  "use strict";

  const config = window.TTUBE_CONFIG || {};
  const apiUrl = String(config.emailApiUrl || "").trim();
  const form = document.getElementById("admin-login");
  const tokenInput = document.getElementById("admin-token");
  const status = document.getElementById("admin-status");
  const content = document.getElementById("admin-content");
  const rowsNode = document.getElementById("email-rows");
  const emptyNode = document.getElementById("empty-state");
  const refreshButton = document.getElementById("refresh-data");
  const exportButton = document.getElementById("export-xlsx");

  let records = [];
  let currentToken = "";

  const endpointReady = () =>
    /^https:\/\/script\.google\.com\/.+\/exec(?:\?.*)?$/.test(apiUrl) &&
    !apiUrl.includes("YOUR_GOOGLE_APPS_SCRIPT_WEB_APP_URL");

  const setStatus = (message, type) => {
    status.textContent = message;
    status.className = "admin-status" + (type ? " " + type : "");
  };

  const jsonp = (params) => new Promise((resolve, reject) => {
    const callbackName = "__ttube_admin_" + Date.now() + "_" + Math.floor(Math.random() * 100000);
    const script = document.createElement("script");
    const url = new URL(apiUrl);

    Object.entries(params).forEach(([key, value]) => url.searchParams.set(key, value));
    url.searchParams.set("callback", callbackName);

    const cleanup = () => {
      delete window[callbackName];
      script.remove();
      clearTimeout(timer);
    };

    const timer = setTimeout(() => {
      cleanup();
      reject(new Error("timeout"));
    }, 12000);

    window[callbackName] = (payload) => {
      cleanup();
      resolve(payload);
    };

    script.onerror = () => {
      cleanup();
      reject(new Error("network"));
    };

    script.src = url.toString();
    document.head.appendChild(script);
  });

  const formatDateTime = (value) => {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value || "—");
    return new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit", month: "2-digit", year: "numeric",
      hour: "2-digit", minute: "2-digit"
    }).format(date);
  };

  const render = () => {
    rowsNode.replaceChildren();

    records.forEach((record) => {
      const tr = document.createElement("tr");
      [formatDateTime(record.timestamp), record.email, record.source || "—", record.consent || "yes"].forEach((value) => {
        const td = document.createElement("td");
        td.textContent = value;
        tr.appendChild(td);
      });
      rowsNode.appendChild(tr);
    });

    emptyNode.hidden = records.length !== 0;
    document.getElementById("count-total").textContent = String(records.length);

    const now = new Date();
    const todayKey = [now.getFullYear(), now.getMonth(), now.getDate()].join("-");
    const todayCount = records.filter((record) => {
      const date = new Date(record.timestamp);
      return !Number.isNaN(date.getTime()) &&
        [date.getFullYear(), date.getMonth(), date.getDate()].join("-") === todayKey;
    }).length;
    document.getElementById("count-today").textContent = String(todayCount);
    document.getElementById("latest-time").textContent = records.length ? formatDateTime(records[0].timestamp) : "—";
  };

  const loadData = async () => {
    if (!endpointReady()) {
      setStatus("Chưa cấu hình emailApiUrl trong config.js.", "error");
      return;
    }

    setStatus("Đang tải dữ liệu…");

    try {
      const payload = await jsonp({ action: "list", token: currentToken });
      if (!payload || payload.ok !== true) {
        throw new Error(payload && payload.error ? payload.error : "unauthorized");
      }
      records = Array.isArray(payload.records) ? payload.records : [];
      content.hidden = false;
      render();
      setStatus("Đã tải " + records.length + " email.", "success");
    } catch (error) {
      content.hidden = true;
      const message = error.message === "unauthorized" ? "Admin token không đúng." : "Không thể tải dữ liệu. Kiểm tra endpoint và token.";
      setStatus(message, "error");
    }
  };

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    currentToken = tokenInput.value.trim();
    if (!currentToken) return;
    loadData();
  });

  refreshButton.addEventListener("click", loadData);

  exportButton.addEventListener("click", () => {
    if (!records.length) {
      setStatus("Chưa có dữ liệu để xuất.", "error");
      return;
    }

    const data = records.map((record, index) => ({
      STT: index + 1,
      "Thời gian": formatDateTime(record.timestamp),
      Email: record.email,
      "Nguồn": record.source || "",
      "Đồng ý nhận tin": record.consent || "yes"
    }));

    if (window.XLSX && window.XLSX.utils) {
      const sheet = window.XLSX.utils.json_to_sheet(data);
      sheet["!cols"] = [{ wch: 8 }, { wch: 22 }, { wch: 34 }, { wch: 18 }, { wch: 18 }];
      const book = window.XLSX.utils.book_new();
      window.XLSX.utils.book_append_sheet(book, sheet, "T Tube Emails");
      const stamp = new Date().toISOString().slice(0, 10);
      window.XLSX.writeFile(book, "T-Tube-emails-" + stamp + ".xlsx");
      setStatus("Đã xuất file Excel.", "success");
      return;
    }

    const header = ["STT", "Thời gian", "Email", "Nguồn", "Đồng ý nhận tin"];
    const escape = (value) => '"' + String(value == null ? "" : value).replace(/"/g, '""') + '"';
    const csv = "\uFEFF" + [header, ...data.map((row) => Object.values(row))].map((row) => row.map(escape).join(",")).join("\r\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "T-Tube-emails.csv";
    link.click();
    URL.revokeObjectURL(link.href);
    setStatus("Thư viện Excel không tải được nên đã xuất CSV tương thích Excel.", "success");
  });
})();
