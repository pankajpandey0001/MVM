document.addEventListener("DOMContentLoaded", () => {
    const role = API.getUserRole();
    if (!role) {
        API.logout();
        return;
    }

    const navGov = document.getElementById("navGov");
    const navOps = document.getElementById("navOps");
    const logoutBtn = document.getElementById("logoutBtn");
    const adminCategorySection = document.getElementById("adminCategorySection");
    const catParent = document.getElementById("catParent");
    const prodCategory = document.getElementById("prodCategory");
    const categoryForm = document.getElementById("categoryForm");
    const productForm = document.getElementById("productForm");
    const catalogTableBody = document.getElementById("catalogTableBody");

    const tabActiveCatalog = document.getElementById("tabActiveCatalog");
    const tabApprovalQueue = document.getElementById("tabApprovalQueue");
    const tabBannedItems = document.getElementById("tabBannedItems");

    let currentView = "active";

    // Show Governance link and Category Section for both SUPER_ADMIN and ADMIN
    if (role === "SUPER_ADMIN" || role === "ADMIN") {
        if (navGov) {
            navGov.style.display = "inline-block";
            navGov.textContent = role === "ADMIN" ? "Vendor Governance" : "User Governance";
        }
        if (adminCategorySection) adminCategorySection.style.display = "block";
    } else if (role === "VENDOR") {
        if (navOps) navOps.style.display = "inline-block";
        const adminPriceCorridor = document.getElementById("adminPriceCorridorGroup");
        const prodMinPrice = document.getElementById("prodMinPrice");
        const prodMrp = document.getElementById("prodMrp");
        const formTitle = document.getElementById("productFormTitle");
        const saveBtn = document.getElementById("saveProductBtn");

        if (adminPriceCorridor) adminPriceCorridor.style.display = "none";
        if (prodMinPrice) prodMinPrice.removeAttribute("required");
        if (prodMrp) prodMrp.removeAttribute("required");

        if (formTitle) formTitle.textContent = "Propose New Master Product Blueprint";
        if (saveBtn) saveBtn.textContent = "Submit Proposal for Admin Review";
    }

    if (logoutBtn) {
        logoutBtn.addEventListener("click", (e) => {
            e.preventDefault();
            API.logout();
        });
    }

    async function loadCategoryDropdowns() {
        try {
            let allCategories;
            try {
                allCategories = (await API.get("/api/catalog/categories")) || [];
            } catch {
                allCategories = (await API.get("/api/catalog/categories/roots")) || [];
            }

            let rootCategories;
            try {
                rootCategories = (await API.get("/api/catalog/categories/roots")) || [];
            } catch {
                rootCategories = allCategories.filter((c) => !c.parentId);
            }

            if (catParent) {
                catParent.innerHTML = `<option value="">-- None (Create Root) --</option>` +
                    rootCategories.map(c => `<option value="${c.id}">${escapeHtml(c.name)} (${escapeHtml(c.code)})</option>`).join("");
            }

            if (prodCategory) {
                if (allCategories.length === 0) {
                    prodCategory.innerHTML = `<option value="" disabled selected>No categories available</option>`;
                } else {
                    prodCategory.innerHTML = `<option value="" disabled selected>-- Select Category --</option>` +
                        allCategories.map(c => {
                            const label = c.parentCategoryName ? `${c.parentCategoryName} → ${c.name}` : c.name;
                            const codeSuffix = c.code ? ` (${c.code})` : "";
                            return `<option value="${c.id}">${escapeHtml(label)}${escapeHtml(codeSuffix)}</option>`;
                        }).join("");
                }
            }
        } catch (err) {
            console.error("Failed to populate category dropdowns:", err);
            if (prodCategory) {
                prodCategory.innerHTML = `<option value="" disabled selected>Failed to load categories</option>`;
            }
        }
    }

    async function loadCatalogFeed() {
        if (!catalogTableBody) return;
        catalogTableBody.innerHTML = `<tr><td colspan="6" style="text-align: center;">Loading catalog items...</td></tr>`;

        let url = "/api/catalog/products/active?page=0&size=50";
        if (currentView === "approval") url = "/api/catalog/products/approval-queue?page=0&size=50";
        if (currentView === "banned") url = "/api/catalog/products/banned?page=0&size=50";

        try {
            const data = await API.get(url);
            const items = data["content"] || [];
            if (items.length === 0) {
                catalogTableBody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No products found in this feed.</td></tr>`;
                return;
            }

            catalogTableBody.innerHTML = items.map(p => {
                const isApproved = Boolean(p["isApproved"]);
                const isBanned = Boolean(p["isBanned"]);
                const categoryObj = p["category"] || {};
                const catName = categoryObj["name"] || "Uncategorized";
                const priceCorridor = (p["minPrice"] != null && p["mrp"] != null)
                    ? `₹${p["minPrice"]} - ₹${p["mrp"]}`
                    : `<em>Pending Admin Pricing</em>`;

                return `
                    <tr>
                        <td><code>${escapeHtml(p["sku"] || "N/A")}</code></td>
                        <td><strong>${escapeHtml(p["title"] || "")}</strong></td>
                        <td>${escapeHtml(catName)}</td>
                        <td>${priceCorridor}</td>
                        <td>
                            ${!isApproved
                    ? `<span class="badge badge-PENDING">Review Pending</span>`
                    : isBanned
                        ? `<span class="badge badge-REJECTED">Banned</span>`
                        : `<span class="badge badge-ACTIVE">Active</span>`}
                        </td>
                        <td>
                            ${(role === "SUPER_ADMIN" || role === "ADMIN") ? `
                                ${!isApproved ? `
                                    <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px; background: var(--state-active);" onclick="adminReviewPrompt(${p["id"]}, '${escapeHtml(p["title"])}')">Approve & Price</button>
                                    <button type="button" class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;" onclick="rejectProduct(${p["id"]})">Reject</button>
                                ` : `
                                    <button type="button" class="btn ${isBanned ? 'btn-secondary' : 'btn-danger'}" style="padding: 4px 8px; font-size: 12px;" onclick="toggleBan(${p["id"]}, ${!isBanned})">
                                        ${isBanned ? 'Unban' : 'Ban Product'}
                                    </button>
                                `}
                            ` : (role === "VENDOR" && isApproved && !isBanned) ? `
                                <button type="button" class="btn" style="padding: 4px 8px; font-size: 12px;" onclick="openSellModal(${p["id"]}, '${escapeHtml(p["title"])}', ${p["minPrice"]}, ${p["mrp"]})">
                                    + Add to My Inventory
                                </button>
                            ` : `<em>Standard View</em>`}
                        </td>
                    </tr>
                `;
            }).join("");
        } catch (err) {
            catalogTableBody.innerHTML = `<tr><td colspan="6" style="color: var(--state-rejected); text-align: center;">${escapeHtml(err.message || "Failed to load catalog")}</td></tr>`;
        }
    }

    window.adminReviewPrompt = async (prodId, title) => {
        const minPriceStr = prompt(`Set Minimum Allowed Price (₹) for "${title}":`, "100");
        if (!minPriceStr) return;
        const minPrice = parseFloat(minPriceStr);

        const mrpStr = prompt(`Set Maximum Price / MRP (₹) for "${title}":`, "200");
        if (!mrpStr) return;
        const mrp = parseFloat(mrpStr);

        if (isNaN(minPrice) || isNaN(mrp) || minPrice <= 0 || mrp <= 0 || minPrice > mrp) {
            alert("Invalid price corridor! Ensure Min Price > 0, MRP > 0, and Min Price <= MRP.");
            return;
        }

        try {
            await API.patch(`/api/catalog/products/${prodId}/review`, {
                approve: true,
                minPrice: minPrice,
                mrp: mrp
            });
            alert("Product blueprint approved and published to active catalog!");
            await loadCatalogFeed();
        } catch (err) {
            alert(err.message);
        }
    };

    window.rejectProduct = async (prodId) => {
        if (!confirm("Are you sure you want to reject and permanently delete this blueprint proposal?")) return;
        try {
            await API.patch(`/api/catalog/products/${prodId}/review`, {
                approve: false
            });
            alert("Proposal rejected and permanently removed from database.");
            await loadCatalogFeed();
        } catch (err) {
            alert(err.message);
        }
    };

    window.openSellModal = async (prodId, title, minPrice, mrp) => {
        const priceStr = prompt(`Set your Selling Price for "${title}"\nAllowed Range: ₹${minPrice} - ₹${mrp}:`);
        if (!priceStr) return;

        const sellingPrice = parseFloat(priceStr);
        if (isNaN(sellingPrice) || sellingPrice < minPrice || sellingPrice > mrp) {
            alert(`Price out of bounds! Selling price must be between ₹${minPrice} and ₹${mrp}.`);
            return;
        }

        const stockStr = prompt(`Enter initial stock quantity to inward for "${title}":`, "10");
        if (!stockStr) return;

        const initialStock = parseInt(stockStr, 10);
        if (isNaN(initialStock) || initialStock <= 0) {
            alert("Invalid stock quantity! Must be at least 1.");
            return;
        }

        try {
            await API.post("/api/catalog/products/sell-existing", {
                masterProductId: prodId,
                sellingPrice: sellingPrice,
                initialStock: initialStock
            });
            alert(`"${title}" has been successfully added to your inventory!`);
            await loadCatalogFeed();
        } catch (err) {
            alert(err.message);
        }
    };

    window.toggleBan = async (prodId, ban) => {
        try {
            await API.patch(`/api/catalog/products/${prodId}/ban?ban=${ban}`);
            await loadCatalogFeed();
        } catch (err) {
            alert(err.message);
        }
    };

    if (categoryForm) {
        categoryForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = {
                name: document.getElementById("catName").value.trim(),
                code: document.getElementById("catCode").value.trim().toUpperCase(),
                parentId: catParent && catParent.value ? parseInt(catParent.value, 10) : null
            };

            try {
                await API.post("/api/catalog/categories", payload);
                categoryForm.reset();
                await loadCategoryDropdowns();
                alert("Category created successfully.");
            } catch (err) {
                alert(err.message);
            }
        });
    }

    if (productForm) {
        productForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            if (!prodCategory || !prodCategory.value) {
                alert("Please select a category.");
                return;
            }

            const title = document.getElementById("prodTitle").value.trim();
            const description = document.getElementById("prodDesc").value.trim();
            const categoryId = parseInt(prodCategory.value, 10);

            if (role === "VENDOR") {
                const payload = {
                    title: title,
                    description: description,
                    categoryId: categoryId
                };

                try {
                    await API.post("/api/catalog/products/propose", payload);
                    productForm.reset();
                    alert("Proposal submitted to Admin Queue.");
                    await loadCatalogFeed();
                } catch (err) {
                    alert(err.message);
                }
            } else {
                const minPrice = parseFloat(document.getElementById("prodMinPrice").value);
                const mrp = parseFloat(document.getElementById("prodMrp").value);

                if (isNaN(minPrice) || isNaN(mrp) || minPrice > mrp) {
                    alert("Validation error: Min Price cannot exceed MRP");
                    return;
                }

                const payload = {
                    title: title,
                    description: description,
                    categoryId: categoryId,
                    minPrice: minPrice,
                    mrp: mrp
                };

                try {
                    await API.post("/api/catalog/products", payload);
                    productForm.reset();
                    alert("Master product created.");
                    await loadCatalogFeed();
                } catch (err) {
                    alert(err.message);
                }
            }
        });
    }

    if (tabActiveCatalog) {
        tabActiveCatalog.addEventListener("click", async () => {
            currentView = "active";
            tabActiveCatalog.className = "btn";
            if (tabApprovalQueue) tabApprovalQueue.className = "btn btn-secondary";
            if (tabBannedItems) tabBannedItems.className = "btn btn-secondary";
            await loadCatalogFeed();
        });
    }

    if (tabApprovalQueue) {
        tabApprovalQueue.addEventListener("click", async () => {
            currentView = "approval";
            if (tabActiveCatalog) tabActiveCatalog.className = "btn btn-secondary";
            tabApprovalQueue.className = "btn";
            if (tabBannedItems) tabBannedItems.className = "btn btn-secondary";
            await loadCatalogFeed();
        });
    }

    if (tabBannedItems) {
        tabBannedItems.addEventListener("click", async () => {
            currentView = "banned";
            if (tabActiveCatalog) tabActiveCatalog.className = "btn btn-secondary";
            if (tabApprovalQueue) tabApprovalQueue.className = "btn btn-secondary";
            tabBannedItems.className = "btn";
            await loadCatalogFeed();
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
            await loadCategoryDropdowns();
            await loadCatalogFeed();
        } catch (e) {
            console.error("Catalog init error:", e);
        }
    })();
});