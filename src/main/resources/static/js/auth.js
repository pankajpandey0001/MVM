document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    const registerForm = document.getElementById("registerForm");
    const otpForm = document.getElementById("otpForm");
    const showRegisterBtn = document.getElementById("showRegisterBtn");
    const cancelRegisterBtn = document.getElementById("cancelRegisterBtn");
    const otpModal = document.getElementById("otpModal");
    const closeOtpModal = document.getElementById("closeOtpModal");
    const alertBox = document.getElementById("alertBox");

    let pendingRegistrationEmail = "";

    function showAlert(message, isError = true) {
        if (!alertBox) return;
        alertBox.style.display = "block";
        alertBox.style.backgroundColor = isError ? "#fee2e2" : "#dcfce7";
        alertBox.style.color = isError ? "#dc2626" : "#16a34a";
        alertBox.textContent = message;
    }

    if (showRegisterBtn && loginForm && registerForm && alertBox) {
        showRegisterBtn.addEventListener("click", () => {
            loginForm.style.display = "none";
            showRegisterBtn.style.display = "none";
            registerForm.style.display = "block";
            alertBox.style.display = "none";
        });
    }

    if (cancelRegisterBtn && loginForm && registerForm && showRegisterBtn && alertBox) {
        cancelRegisterBtn.addEventListener("click", () => {
            registerForm.style.display = "none";
            loginForm.style.display = "block";
            showRegisterBtn.style.display = "block";
            alertBox.style.display = "none";
        });
    }

    if (closeOtpModal && otpModal) {
        closeOtpModal.addEventListener("click", () => {
            otpModal.classList.add("modal-hidden");
        });
    }

    // Handle User Login
    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const emailInput = document.getElementById("loginEmail");
            const passwordInput = document.getElementById("loginPassword");

            if (!emailInput || !passwordInput) return;

            const payload = {
                email: emailInput.value.trim(),
                password: passwordInput.value
            };

            try {
                const response = await fetch("/api/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });

                const data = await response.json();
                if (!response.ok) {
                    showAlert(data["error"] || "Authentication failed.");
                    return;
                }

                localStorage.setItem("authToken", data["token"]);
                localStorage.setItem("userRole", data["role"]);
                localStorage.setItem("userEmail", data["email"]);

                const role = data["role"];
                // Redirect SUPER_ADMIN and ADMIN to Governance Hub
                if (role === "SUPER_ADMIN" || role === "ADMIN") {
                    window.location.href = "/governance.html";
                } else if (role === "VENDOR") {
                    window.location.href = "/operations.html";
                } else {
                    window.location.href = "/catalog.html";
                }
            } catch {
                showAlert("Unable to connect to the authentication server.");
            }
        });
    }

    // Handle Registration & OTP Dispatch with UI Locking
    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const submitBtn = registerForm.querySelector('button[type="submit"]');
            const originalText = submitBtn ? submitBtn.textContent : "Send Email OTP Verification";

            const fullNameInput = document.getElementById("regFullName");
            const emailInput = document.getElementById("regEmail");
            const phoneInput = document.getElementById("regPhone");
            const roleInput = document.getElementById("regRole");
            const passwordInput = document.getElementById("regPassword");

            if (!fullNameInput || !emailInput || !phoneInput || !roleInput || !passwordInput) return;

            const payload = {
                fullName: fullNameInput.value.trim(),
                email: emailInput.value.trim(),
                phone: phoneInput.value.trim(),
                role: roleInput.value,
                password: passwordInput.value
            };

            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = "Sending Verification OTP...";
            }

            try {
                const response = await fetch("/api/auth/register", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });

                const data = await response.json();
                if (!response.ok) {
                    const validationErrors = data["validationErrors"];
                    const validationMsg = validationErrors
                        ? Object.values(validationErrors).join(", ")
                        : data["error"];
                    showAlert(validationMsg || "Registration request failed.");
                    return;
                }

                pendingRegistrationEmail = payload.email;
                if (otpModal) {
                    otpModal.classList.remove("modal-hidden");
                }
                showAlert(data["message"] || "OTP sent successfully.", false);
            } catch {
                showAlert("Error connecting to server for OTP dispatch.");
            } finally {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.textContent = originalText;
                }
            }
        });
    }

    // Handle OTP Submission & Final Registration
    if (otpForm) {
        otpForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const otpInput = document.getElementById("otpCode");
            if (!otpInput) return;

            const payload = {
                email: pendingRegistrationEmail,
                otp: otpInput.value.trim()
            };

            try {
                const response = await fetch("/api/auth/verify-otp", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });

                const data = await response.json();
                if (!response.ok) {
                    showAlert(data["error"] || "Invalid verification code.");
                    return;
                }

                if (otpModal) {
                    otpModal.classList.add("modal-hidden");
                }
                if (registerForm) {
                    registerForm.reset();
                }
                if (cancelRegisterBtn) {
                    cancelRegisterBtn.click();
                }
                showAlert(data["message"] || "Registration successful. Awaiting admin approval.", false);
            } catch {
                showAlert("Failed to verify OTP code.");
            }
        });
    }
});