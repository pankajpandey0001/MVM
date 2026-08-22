document.addEventListener("DOMContentLoaded", () => {
    const role = API.getUserRole();
    if (!role) {
        API.logout();
        return;
    }

    const urlParams = new URLSearchParams(window.location.search);
    const targetVendorId = urlParams.get("vendorId");

    // Admins inspecting a vendor require ?vendorId=X; Vendors view their own dashboard
    const isInspectionMode = (role === "SUPER_ADMIN" || role === "ADMIN") && Boolean(targetVendorId);

    if (role === "VENDOR" && isInspectionMode) {
        window.location.href = "/operations";
        return;
    }

    const kpiMrp = document.getElementById("kpiMrp");
    const kpiSelling = document.getElementById("kpiSelling");
    const kpiActive = document.getElementById("kpiActive");
    const kpiLowStock = document.getElementById("kpiLowStock");

    const inventoryTableBody = document.getElementById("inventoryTableBody");
    const ledgerTableBody = document.getElementById("ledgerTableBody");
    const refreshStockBtn = document.getElementById("refreshStockBtn");
    const logoutBtn = document.getElementById("logoutBtn");

    const stockModal = document.getElementById("stockModal");
    const closeStockModal = document.getElementById("closeStockModal");
    const stockForm = document.getElementById("stockForm");
    const stockProductId = document.getElementById("stockProductId");
    const stockEntryType = document.getElementById("stockEntryType");
    const stockQuantity = document.getElementById("stockQuantity");
    const stockReason = document.getElementById("stockReason");

    const priceModal = document.getElementById("priceModal");
    const closePriceModal = document.getElementById("closePriceModal");
    const priceForm = document.getElementById("priceForm");
    const priceProductId = document.getElementById("priceProductId");
    const newSellingPrice = document.getElementById("newSellingPrice");
    const priceCorridorNotice = document.getElementById("priceCorridorNotice");

    if (logoutBtn) {
        logoutBtn.addEventListener("click", (e) => {
            e.preventDefault();
            API.logout();
        });
    }

    if (closeStockModal && stockModal) {
        closeStockModal.addEventListener("click", () => stockModal.classList.add("modal-hidden"));
    }
    if (closePriceModal && priceModal) {
        closePriceModal.addEventListener("click", () => priceModal.classList.add("modal-hidden"));
    }

    async function loadDashboardKPIs() {
        if (!kpiMrp || !kpiSelling || !kpiActive || !kpiLowStock) return;
        try {
            const endpoint = isInspectionMode
                ? `/api/vendor/operations/admin/vendor/${targetVendorId}/dashboard`
                : `/api/vendor/operations/dashboard`;

            const data = await API.get(endpoint);
            const valMrp = Number(data?.["totalValuationAtMrp"] || 0);
            const valSelling = Number(data?.["totalValuationAtSellingPrice"] || 0);

            kpiMrp.textContent = `₹${valMrp.toFixed(2)}`;
            kpiSelling.textContent = `₹${valSelling.toFixed(2)}`;
            kpiActive.textContent = String(data?.["totalActiveItemsCount"] || 0);
            kpiLowStock.textContent = String(data?.["lowStockItemsCount"] || 0);
        } catch (err) {
            console.error("Failed to load dashboard KPIs:", err);
        }
    }

    async function loadInventory() {
        if (!inventoryTableBody) return;
        inventoryTableBody.innerHTML = `<tr><td colspan="7" style="text-align:center;">Loading store inventory...</td></tr>`;
        try {
            const endpoint = isInspectionMode
                ? `/api/vendor/operations/admin/vendor/${targetVendorId}/inventory?page=0&size=50`
                : `/api/vendor/operations/inventory?page=0&size=50`;

            const data = await API.get(endpoint);
            const items = data?.["content"] || [];
            if (items.length === 0) {
                inventoryTableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">No inventory products added yet. Link products from the Master Catalog.</td></tr>`;
                return;
            }

            inventoryTableBody.innerHTML = items.map(inv => {
                const prod = inv?.["masterProduct"] || {};
                const isBanned = Boolean(prod["isBanned"]);
                const minPrice = prod["minPrice"] != null ? `₹${prod["minPrice"]}` : 'N/A';
                const mrp = prod["mrp"] != null ? `₹${prod["mrp"]}` : 'N/A';
                const sellingPrice = inv["sellingPrice"] != null ? `₹${inv["sellingPrice"]}` : 'N/A';
                const currentStock = Number(inv["currentStock"] || 0);
                const prodId = prod["id"];
                const prodTitle = prod["title"] || "Untitled";

                return `
                    <tr style="${isBanned ? 'background: #fff1f2;' : ''}">
                        <td><code>${escapeHtml(prod["sku"] || 'N/A')}</code></td>
                        <td><strong>${escapeHtml(prodTitle)}</strong></td>
                        <td>${minPrice} - ${mrp}</td>
                        <td><strong>${sellingPrice}</strong></td>
                        <td>
                            <strong style="color: ${currentStock <= 10 ? 'var(--state-rejected)' : 'inherit'};">
                                ${currentStock} Units
                            </strong>
                        </td>
                        <td>
                            ${isBanned
                    ? `<span class="badge badge-REJECTED">Banned</span>`
                    : `<span class="badge badge-ACTIVE">Active</span>`}
                        </td>
                        <td>
                            ${isInspectionMode ? `
                                <em>Admin Read-Only</em>
                            ` : isBanned ? `
                                <button type="button" class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;" onclick="clearBanned(${prodId})">Remove Stock from Store</button>
                            ` : `
                                <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px;" onclick="openStockModal(${prodId}, '${escapeHtml(prodTitle)}')">Adjust Stock</button>
                                <button type="button" class="btn btn-secondary" style="padding: 4px 8px; font-size: 12px;" onclick="openPriceModal(${prodId}, ${prod["minPrice"]}, ${prod["mrp"]}, ${inv["sellingPrice"]})">Update Price</button>
                            `}
                        </td>
                    </tr>
                `;
            }).join("");
        } catch (err) {
            inventoryTableBody.innerHTML = `<tr><td colspan="7" style="color: var(--state-rejected); text-align: center;">${escapeHtml(err.message || "Failed to load inventory")}</td></tr>`;
        }
    }

    async function loadLedger() {
        if (!ledgerTableBody) return;
        ledgerTableBody.innerHTML = `<tr><td colspan="6" style="text-align: center;">Loading transaction trail...</td></tr>`;
        try {
            const data = await API.get("/api/vendor/operations/ledger/trail?page=0&size=20");
            const entries = data?.["content"] || [];
            if (entries.length === 0) {
                ledgerTableBody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No stock movements recorded yet.</td></tr>`;
                return;
            }

            ledgerTableBody.innerHTML = entries.map(entry => {
                const invObj = entry?.["vendorInventory"] || {};
                const prod = invObj?.["masterProduct"] || {};
                const qtyChange = Number(entry?.["quantityChange"] || 0);
                const isPositive = qtyChange > 0;
                const balanceAfter = entry?.["balanceAfter"] ?? 0;

                return `
                    <tr>
                        <td>${new Date(entry?.["timestamp"]).toLocaleString()}</td>
                        <td><strong>${escapeHtml(prod["title"] || 'N/A')}</strong> <br><small><code>${escapeHtml(prod["sku"] || 'N/A')}</code></small></td>
                        <td><span class="badge badge-ACTIVE">${escapeHtml(entry?.["entryType"] || 'N/A')}</span></td>
                        <td style="color: ${isPositive ? 'var(--state-active)' : 'var(--state-rejected)'}; font-weight: bold;">
                            ${isPositive ? '+' : ''}${qtyChange}
                        </td>
                        <td><strong>${balanceAfter} Units</strong></td>
                        <td>${escapeHtml(entry?.["reason"] || 'Manual Adjustment')}</td>
                    </tr>
                `;
            }).join("");
        } catch (err) {
            ledgerTableBody.innerHTML = `<tr><td colspan="6" style="color: var(--state-rejected); text-align: center;">${escapeHtml(err.message || "Failed to load ledger")}</td></tr>`;
        }
    }

    window.openStockModal = (prodId, title) => {
        if (!stockModal) return;
        stockProductId.value = prodId;
        const titleEl = document.getElementById("stockModalTitle");
        if (titleEl) titleEl.textContent = `Stock Movement: ${title}`;
        stockQuantity.value = "";
        stockReason.value = "";
        stockModal.classList.remove("modal-hidden");
    };

    window.openPriceModal = (prodId, min, mrp, current) => {
        if (!priceModal) return;
        priceProductId.value = prodId;
        newSellingPrice.value = current;
        newSellingPrice.min = min;
        newSellingPrice.max = mrp;
        if (priceCorridorNotice) priceCorridorNotice.textContent = `Allowed price corridor: ₹${min} to ₹${mrp} (MRP)`;
        priceModal.classList.remove("modal-hidden");
    };

    window.clearBanned = async (prodId) => {
        if (!confirm("Are you sure you want to clear this banned product from your store view?")) return;
        try {
            await API.delete(`/api/vendor/operations/banned-products/${prodId}/clear`);
            await loadDashboardKPIs();
            await loadInventory();
        } catch (err) {
            alert(err.message);
        }
    };

    if (stockForm) {
        stockForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = {
                masterProductId: parseInt(stockProductId.value, 10),
                entryType: stockEntryType.value,
                quantity: parseInt(stockQuantity.value, 10),
                reason: stockReason.value.trim()
            };

            try {
                await API.post("/api/vendor/operations/stock/adjust", payload);
                if (stockModal) stockModal.classList.add("modal-hidden");
                await loadDashboardKPIs();
                await loadInventory();
                await loadLedger();
            } catch (err) {
                alert(err.message);
            }
        });
    }

    if (priceForm) {
        priceForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = {
                masterProductId: parseInt(priceProductId.value, 10),
                newSellingPrice: parseFloat(newSellingPrice.value)
            };

            try {
                await API.patch("/api/vendor/operations/price/update", payload);
                if (priceModal) priceModal.classList.add("modal-hidden");
                await loadDashboardKPIs();
                await loadInventory();
            } catch (err) {
                alert(err.message);
            }
        });
    }

    if (refreshStockBtn) {
        refreshStockBtn.addEventListener("click", async () => {
            await loadDashboardKPIs();
            await loadInventory();
            await loadLedger();
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
            await loadDashboardKPIs();
            await loadInventory();
            await loadLedger();
        } catch (e) {
            console.error("Operations init error:", e);
        }
    })();
});