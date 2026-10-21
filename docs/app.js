(() => {
  "use strict";

  const config = window.TTUBE_CONFIG || {};
  const apiUrl = String(config.emailApiUrl || "").trim();
  const forms = document.querySelectorAll(".signup-form");
  const navToggle = document.querySelector(".nav-toggle");
  const nav = document.querySelector(".site-nav");

  const endpointReady = () =>
    /^https:\/\/script\.google\.com\/.+\/exec(?:\?.*)?$/.test(apiUrl) &&
    !apiUrl.includes("YOUR_GOOGLE_APPS_SCRIPT_WEB_APP_URL");

  const setStatus = (form, message, type) => {
    const node = form.querySelector(".form-status");
    if (!node) return;
    node.textContent = message;
    node.className = "form-status" + (type ? " " + type : "");
  };

  forms.forEach((form) => {
    form.addEventListener("submit", async (event) => {
      event.preventDefault();

      if (!form.reportValidity()) return;

      if (!endpointReady()) {
        setStatus(form, "Biểu mẫu chưa được kết nối máy chủ. Chủ dự án cần cấu hình Google Apps Script.", "error");
        return;
      }

      const emailInput = form.querySelector('input[name="email"]');
      const button = form.querySelector('button[type="submit"]');
      const email = String(emailInput.value || "").trim().toLowerCase();
      const source = form.dataset.source || "landing-page";

      button.disabled = true;
      setStatus(form, "Đang ghi nhận email…");

      try {
        const body = new URLSearchParams();
        body.set("email", email);
        body.set("source", source);
        body.set("consent", "yes");

        await fetch(apiUrl, {
          method: "POST",
          mode: "no-cors",
          headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
          body
        });

        localStorage.setItem("ttube_waitlist_email", email);
        form.reset();
        setStatus(form, "Đã đăng ký! Cảm ơn bạn — T Tube sẽ gửi tin khi có bản cập nhật đáng chú ý.", "success");
      } catch (error) {
        setStatus(form, "Không thể gửi lúc này. Vui lòng thử lại sau.", "error");
      } finally {
        button.disabled = false;
      }
    });
  });

  if (navToggle && nav) {
    navToggle.addEventListener("click", () => {
      const expanded = navToggle.getAttribute("aria-expanded") === "true";
      navToggle.setAttribute("aria-expanded", String(!expanded));
      nav.classList.toggle("open", !expanded);
    });

    nav.querySelectorAll("a").forEach((link) => {
      link.addEventListener("click", () => {
        nav.classList.remove("open");
        navToggle.setAttribute("aria-expanded", "false");
      });
    });
  }

  const revealNodes = document.querySelectorAll(".reveal");
  if ("IntersectionObserver" in window && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12, rootMargin: "0px 0px -24px" });

    revealNodes.forEach((node) => observer.observe(node));
  } else {
    revealNodes.forEach((node) => node.classList.add("is-visible"));
  }
})();
