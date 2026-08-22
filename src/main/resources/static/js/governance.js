document.addEventListener("DOMContentLoaded", () => {
    const userRole = typeof API !== "undefined" && API.getUserRole ? API.getUserRole() : null;

    // FIX: Only log out if NOT Superadmin AND NOT Admin
    if (!userRole || (userRole !== "SUPER_ADMIN" && userRole !== "ADMIN")) {
        API.logout();
        return;
    }

    const isAdmin = userRole === "ADMIN";

    const statusTabsContainer = document.getElementById("statusTabs");
    const userTableBody = document.getElementById("userTableBody");
    const roleFilter = document.getElementById("roleFilter");
    const refreshBtn = document.getElementById("refreshBtn");
    const logoutBtn = document.getElementById("logoutBtn");

    // Hide or lock the role filter for ADMIN so they only see Vendors
    if (isAdmin && roleFilter) {
        roleFilter.value = "VENDOR";
        const roleFilterContainer = roleFilter.closest("#roleFilterContainer") || roleFilter.parentElement;
        if (roleFilterContainer) {
            roleFilterContainer.style.display = "none";
        }
    }

    const actionModal = document.getElementById("actionModal");
    const closeActionModal = document.getElementById("closeActionModal");
    const actionForm = document.getElementById("actionForm");
    const actionModalTitle = document.getElementById("actionModalTitle");
    const targetUserIdInput = document.getElementById("targetUserId");
    const targetStateInput = document.getElementById("targetState");
    const actionReasonInput = document.getElementById("actionReason");

    const auditModal = document.getElementById("auditModal");
    const closeAuditModal = document.getElementById("closeAuditModal");
    const auditTableBody = document.getElementById("auditTableBody");

    // Vendor Read-Only Modal Elements
    const vendorInventoryModal = document.getElementById("vendorInventoryModal");
    const closeVendorInventoryModal = document.getElementById("closeVendorInventoryModal");
    const vendorInventoryModalTitle = document.getElementById("vendorInventoryModalTitle");
    const vendorInventoryModalSubtitle = document.getElementById("vendorInventoryModalSubtitle");
    const vendorInventoryTableBody = document.getElementById("vendorInventoryTableBody");
    const valMrp = document.getElementById("valMrp");
    const valSelling = document.getElementById("valSelling");
    const valActive = document.getElementById("valActive");
    const valLowStock = document.getElementById("valLowStock");

    const statuses = ["PENDING", "ACTIVE", "SUSPENDED", "DEACTIVATED", "BLACKLISTED", "TERMINATED"];
    let currentActiveStatus = "PENDING";

    if (logoutBtn) {
        logoutBtn.addEventListener("click", (e) => {
            e.preventDefault();
            API.logout();
        });
    }

    if (closeActionModal && actionModal) {
        closeActionModal.addEventListener("click", () => actionModal.classList.add("modal-hidden"));
    }

    if (closeAuditModal && auditModal) {
        closeAuditModal.addEventListener("click", () => auditModal.classList.add("modal-hidden"));
    }

    if (closeVendorInventoryModal && vendorInventoryModal) {
        closeVendorInventoryModal.addEventListener("click", () => vendorInventoryModal.classList.add("modal-hidden"));
    }

    // ------------------------------------------------------------------------
    // Helper: Formatter for "Performed By" to cleanly display Admin Email
    // ------------------------------------------------------------------------
    function formatPerformedByEmail(logItem) {
        if (!logItem) return '<span style="color: var(--text-muted, #999);">System</span>';

        const email = (logItem["performedByEmail"] || logItem["performedBy"] || "").trim();
        if (!email) {
            return '<span style="color: var(--text-muted, #999);">System</span>';
        }

        return `<code>${escapeHtml(email)}</code>`;
    }

    async function loadTabCounts() {
        if (!statusTabsContainer) return;
        try {
            // Send role=VENDOR if current user is ADMIN
            const countUrl = isAdmin
                ? `/api/superadmin/governance/counts?role=VENDOR`
                : `/api/superadmin/governance/counts`;

            const counts = await API.get(countUrl);
            statusTabsContainer.innerHTML = statuses.map((st) => `
            <button type="button" class="btn ${st === currentActiveStatus ? "" : "btn-secondary"}" data-status="${st}" style="padding: 8px 14px; font-size: 13px;">
                ${st} (${counts && counts[st] ? counts[st] : 0})
            </button>
        `).join("");

            statusTabsContainer.querySelectorAll("button").forEach((btn) => {
                btn.addEventListener("click", async () => {
                    const nextStatus = btn.getAttribute("data-status");
                    if (nextStatus) {
                        currentActiveStatus = nextStatus;
                        await loadTabCounts();
                        await loadUsers();
                    }
                });
            });
        } catch (err) {
            console.error("Failed to load tab counts:", err);
        }
    }

    async function loadUsers() {
        if (!userTableBody) return;
        userTableBody.innerHTML = `<tr><td colspan="7" style="text-align: center;">Loading...</td></tr>`;

        // Scope to VENDOR if ADMIN is logged in
        const selectedRole = isAdmin ? "VENDOR" : (roleFilter ? roleFilter.value : "");
        const url = `/api/superadmin/governance/users?status=${currentActiveStatus}${selectedRole ? `&role=${selectedRole}` : ""}&page=0&size=50`;

        try {
            const data = await API.get(url);
            let content = (data && data["content"]) ? data["content"] : [];

            // Filter for ADMIN on client-side safety layer
            if (isAdmin) {
                content = content.filter(u => u["role"] === "VENDOR");
            }

            if (content.length === 0) {
                userTableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted, #666);">No records found under ${currentActiveStatus}.</td></tr>`;
                return;
            }

            userTableBody.innerHTML = content.map((u) => {
                const isVendor = u["role"] === "VENDOR";
                const displayName = escapeHtml(u["fullName"] || u["email"] || `Vendor #${u["id"]}`);
                const userIdStr = String(u["id"]);

                return `
                    <tr>
                        <td><strong>#${userIdStr}</strong></td>
                        <td>${escapeHtml(u["fullName"] || "")}</td>
                        <td>${escapeHtml(u["email"] || "")}</td>
                        <td>${escapeHtml(u["phone"] || "N/A")}</td>
                        <td><span class="badge">${escapeHtml(u["role"] || "")}</span></td>
                        <td><span class="badge badge-${escapeHtml(u["status"] || "")}">${escapeHtml(u["status"] || "")}</span></td>
                        <td>
                            <div style="display: flex; gap: 6px; align-items: center;">
                                ${renderActionButtons(u)}
                                ${isVendor ? `<button type="button" class="btn btn-secondary" style="padding: 4px 8px; font-size: 12px;" data-action="view-inventory" data-id="${userIdStr}" data-name="${displayName}">Inventory</button>` : ""}
                                <button type="button" class="btn btn-secondary" style="padding: 4px 8px; font-size: 12px;" data-action="view-audit" data-id="${userIdStr}">Information</button>
                            </div>
                        </td>
                    </tr>
                `;
            }).join("");
        } catch (err) {
            userTableBody.innerHTML = `<tr><td colspan="7" style="color: var(--state-rejected, #dc2626); text-align: center;">${escapeHtml(err.message || "An error occurred")}</td></tr>`;
        }
    }

    function renderActionButtons(user) {
        if (user["role"] === "SUPER_ADMIN" || (isAdmin && user["role"] !== "VENDOR")) {
            return `<span class="badge" style="background-color: #334155; color: #94a3b8; font-size: 11px; padding: 4px 8px; border-radius: 4px; display: inline-block;">System Protected</span>`;
        }

        const userId = user["id"];
        const status = user["status"];
        const safeName = escapeHtml(user["fullName"] || user["email"] || "User");

        if (status === "PENDING") {
            return `
            <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px; background: var(--state-active, #16a34a);" data-action="open-action" data-id="${userId}" data-state="ACTIVE" data-title="Approve Account">Approve</button>
            <button type="button" class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;" data-action="reject-user" data-id="${userId}" data-name="${safeName}">Reject</button>
            <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px; background: var(--state-blacklisted, #0f172a);" data-action="open-action" data-id="${userId}" data-state="BLACKLISTED" data-title="Blacklist User">Blacklist</button>
        `;
        } else if (status === "ACTIVE") {
            return `
            <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px; background: var(--state-suspended, #d97706);" data-action="open-action" data-id="${userId}" data-state="SUSPENDED" data-title="Suspend Account">Suspend</button>
            <button type="button" class="btn btn-secondary" style="padding: 4px 8px; font-size: 12px;" data-action="open-action" data-id="${userId}" data-state="DEACTIVATED" data-title="Deactivate Account">Deactivate</button>
            <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px; background: var(--state-terminated, #dc2626);" data-action="open-action" data-id="${userId}" data-state="TERMINATED" data-title="Terminate User">Terminate</button>
        `;
        } else if (status === "SUSPENDED" || status === "DEACTIVATED") {
            return `
            <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px; background: var(--state-active, #16a34a);" data-action="open-action" data-id="${userId}" data-state="ACTIVE" data-title="Reactivate Account">Reactivate</button>
            <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px; background: var(--state-terminated, #dc2626);" data-action="open-action" data-id="${userId}" data-state="TERMINATED" data-title="Terminate User">Terminate</button>
        `;
        } else if (status === "BLACKLISTED") {
            return `
            <button type="button" class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;" data-action="reject-user" data-id="${userId}" data-name="${safeName}">Delete & Purge</button>
        `;
        } else if (status === "TERMINATED") {
            return `
            <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px; background: var(--state-active, #16a34a);" data-action="open-action" data-id="${userId}" data-state="ACTIVE" data-title="Reactivate Terminated Account">Approve</button>
        `;
        }
        return `<em style="font-size: 12px; color: var(--text-muted, #666);">Terminal State</em>`;
    }

    function openActionModal(userId, targetState, title) {
        if (!actionModal || !targetUserIdInput || !targetStateInput || !actionModalTitle || !actionReasonInput) return;
        targetUserIdInput.value = userId;
        targetStateInput.value = targetState;
        actionModalTitle.textContent = title;
        actionReasonInput.value = "";
        actionModal.classList.remove("modal-hidden");
    }

    async function rejectAndPurgeUser(userId, identifier) {
        if (!confirm(`Are you sure you want to REJECT and PERMANENTLY DELETE user "${identifier}" from the database? This cannot be undone.`)) {
            return;
        }

        try {
            await API.delete(`/api/superadmin/governance/users/${userId}/reject`);
            alert("User application was rejected and permanently purged from the database.");
            await loadTabCounts();
            await loadUsers();
        } catch (err) {
            alert(err.message || "Failed to purge user");
        }
    }

    async function viewAuditTrail(userId) {
        if (!auditModal || !auditTableBody) return;
        try {
            const logs = await API.get(`/api/superadmin/governance/users/${userId}/audit-trail`);
            const logList = Array.isArray(logs) ? logs : [];
            auditTableBody.innerHTML = logList.length === 0
                ? `<tr><td colspan="4" style="text-align: center;">No audit transitions recorded yet.</td></tr>`
                : logList.map((l) => `
                    <tr>
                        <td>${new Date(l["timestamp"]).toLocaleString()}</td>
                        <td><span class="badge badge-${escapeHtml(l["previousStatus"] || "")}">${escapeHtml(l["previousStatus"] || "")}</span> &rarr; <span class="badge badge-${escapeHtml(l["newStatus"] || "")}">${escapeHtml(l["newStatus"] || "")}</span></td>
                        <td>${escapeHtml(l["actionReason"] || "N/A")}</td>
                        <td>${formatPerformedByEmail(l)}</td>
                    </tr>
                `).join("");
            auditModal.classList.remove("modal-hidden");
        } catch (err) {
            alert(err.message || "Failed to load audit logs");
        }
    }

    async function viewVendorInventory(vendorId, vendorName) {
        if (!vendorInventoryModal || !vendorInventoryTableBody) return;

        if (vendorInventoryModalTitle) {
            vendorInventoryModalTitle.textContent = `Inventory for ${vendorName}`;
        }
        if (vendorInventoryModalSubtitle) {
            vendorInventoryModalSubtitle.textContent = `Vendor Account ID: #${vendorId} (View Only Mode)`;
        }

        vendorInventoryTableBody.innerHTML = `<tr><td colspan="7" style="text-align: center;">Loading vendor catalog & stock data...</td></tr>`;

        if (valMrp) valMrp.textContent = "₹0.00";
        if (valSelling) valSelling.textContent = "₹0.00";
        if (valActive) valActive.textContent = "0";
        if (valLowStock) valLowStock.textContent = "0";

        vendorInventoryModal.classList.remove("modal-hidden");

        try {
            const valuation = await API.get(`/api/superadmin/governance/vendors/${vendorId}/valuation`);
            if (valuation) {
                if (valMrp) valMrp.textContent = `₹${Number(valuation["totalValuationAtMrp"] || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
                if (valSelling) valSelling.textContent = `₹${Number(valuation["totalValuationAtSellingPrice"] || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
                if (valActive) valActive.textContent = String(valuation["totalActiveItemsCount"] || 0);
                if (valLowStock) valLowStock.textContent = String(valuation["lowStockItemsCount"] || 0);
            }

            const inventoryData = await API.get(`/api/superadmin/governance/vendors/${vendorId}/inventory?page=0&size=100`);
            const items = (inventoryData && inventoryData["content"]) ? inventoryData["content"] : [];

            if (items.length === 0) {
                vendorInventoryTableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted, #666);">This vendor has not added any products to their inventory yet.</td></tr>`;
                return;
            }

            vendorInventoryTableBody.innerHTML = items.map((item) => {
                const p = item["masterProduct"] || {};
                const isBanned = Boolean(p["isBanned"]);
                const currentStock = Number(item["currentStock"] || 0);
                const isLowStock = currentStock <= 10 && !isBanned;
                const categoryObj = p["category"] || {};
                const categoryName = categoryObj["name"] || "N/A";

                return `
                    <tr style="${isBanned ? "opacity: 0.6; background-color: #fef2f2;" : ""}">
                        <td>
                            <strong>${escapeHtml(p["title"] || "Untitled Product")}</strong>
                            <div style="font-size: 11px; color: #64748b;">${escapeHtml(p["brand"] || "")}</div>
                        </td>
                        <td>${escapeHtml(categoryName)}</td>
                        <td><code>${escapeHtml(p["sku"] || "N/A")}</code></td>
                        <td>₹${p["minPrice"] || 0} - ₹${p["mrp"] || 0}</td>
                        <td><strong>₹${Number(item["sellingPrice"] || 0).toFixed(2)}</strong></td>
                        <td>
                            <span style="font-weight: bold; color: ${isLowStock ? "#dc2626" : "inherit"};">
                                ${currentStock} units
                            </span>
                            ${isLowStock ? '<span style="font-size: 10px; color: #dc2626; display: block;">Low Stock</span>' : ""}
                        </td>
                        <td>
                            ${isBanned
                    ? '<span class="badge" style="background: #dc2626; color: #fff;">Banned</span>'
                    : '<span class="badge badge-ACTIVE">Active</span>'}
                        </td>
                    </tr>
                `;
            }).join("");

        } catch (err) {
            vendorInventoryTableBody.innerHTML = `<tr><td colspan="7" style="color: var(--state-rejected, #dc2626); text-align: center;">Failed to load inventory: ${escapeHtml(err.message || "Error")}</td></tr>`;
        }
    }

    if (userTableBody) {
        userTableBody.addEventListener("click", async (e) => {
            const target = e.target.closest("[data-action]");
            if (!target) return;

            const action = target.getAttribute("data-action");
            const id = target.getAttribute("data-id");

            try {
                if (action === "view-inventory") {
                    const name = target.getAttribute("data-name") || `Vendor #${id}`;
                    await viewVendorInventory(id, name);
                } else if (action === "view-audit") {
                    await viewAuditTrail(id);
                } else if (action === "open-action") {
                    const state = target.getAttribute("data-state");
                    const title = target.getAttribute("data-title");
                    openActionModal(id, state, title);
                } else if (action === "reject-user") {
                    const name = target.getAttribute("data-name") || "User";
                    await rejectAndPurgeUser(id, name);
                }
            } catch (err) {
                console.error("Action execution error:", err);
            }
        });
    }

    if (actionForm) {
        actionForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!targetUserIdInput || !targetStateInput || !actionReasonInput) return;

            const userId = targetUserIdInput.value;
            const payload = {
                targetStatus: targetStateInput.value,
                reason: actionReasonInput.value.trim()
            };

            if (!payload.reason) {
                alert("Please provide a reason for this status transition.");
                return;
            }

            try {
                await API.patch(`/api/superadmin/governance/users/${userId}/status`, payload);
                if (actionModal) actionModal.classList.add("modal-hidden");
                await loadTabCounts();
                await loadUsers();
            } catch (err) {
                alert(err.message || "Failed to update user status");
            }
        });
    }

    if (roleFilter) {
        roleFilter.addEventListener("change", async () => {
            await loadUsers();
        });
    }

    if (refreshBtn) {
        refreshBtn.addEventListener("click", async () => {
            await loadTabCounts();
            await loadUsers();
        });
    }

    function escapeHtml(str) {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    (async () => {
        try {
            await loadTabCounts();
            await loadUsers();
        } catch (e) {
            console.error("Initialization error:", e);
        }
    })();
});