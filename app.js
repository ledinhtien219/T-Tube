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

  const releaseManifestUrl = "https://raw.githubusercontent.com/ledinhtien219/T-Tube/main/update.json";

  const syncStableRelease = async () => {
    try {
      const response = await fetch(releaseManifestUrl, { cache: "no-store" });
      if (!response.ok) return;

      const release = await response.json();
      if (!release || release.approved !== true || !release.versionName || !release.downloadUrl) return;

      const version = String(release.versionName);
      const build = String(release.versionCode || "");
      const releasePage = "https://github.com/ledinhtien219/T-Tube/releases/tag/v" + encodeURIComponent(version);

      document.querySelectorAll("[data-release-version]").forEach((node) => {
        node.textContent = "v" + version;
      });
      document.querySelectorAll("[data-release-version-plain]").forEach((node) => {
        node.textContent = version;
      });
      document.querySelectorAll("[data-release-build]").forEach((node) => {
        node.textContent = build;
      });
      document.querySelectorAll("[data-release-download]").forEach((node) => {
        node.href = release.downloadUrl;
      });
      document.querySelectorAll("[data-release-page]").forEach((node) => {
        node.href = releasePage;
      });

      const badge = document.querySelector("[data-release-badge]");
      if (badge) {
        const dot = document.createElement("span");
        dot.className = "live-dot";
        badge.replaceChildren(dot, document.createTextNode(" T Tube v" + version + (build ? " · Build " + build : "")));
      }

      const changelog = document.getElementById("release-changelog");
      if (changelog && Array.isArray(release.changelog) && release.changelog.length) {
        const fragment = document.createDocumentFragment();
        release.changelog.slice(0, 6).forEach((item, index) => {
          const article = document.createElement("article");
          article.className = "change-card is-visible";

          const number = document.createElement("span");
          number.textContent = String(index + 1).padStart(2, "0");

          const text = document.createElement("p");
          text.textContent = String(item);

          article.append(number, text);
          fragment.appendChild(article);
        });
        changelog.replaceChildren(fragment);
      }

      const schema = document.querySelector('script[type="application/ld+json"]');
      if (schema) {
        try {
          const data = JSON.parse(schema.textContent);
          data.softwareVersion = version;
          data.downloadUrl = release.downloadUrl;
          schema.textContent = JSON.stringify(data);
        } catch (_) {}
      }
    } catch (_) {
      // Keep the baked-in stable release info when GitHub/raw is temporarily unavailable.
    }
  };

  syncStableRelease();

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
