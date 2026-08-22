const API = {
    getToken() {
        return localStorage.getItem("authToken");
    },

    getUserRole() {
        return localStorage.getItem("userRole");
    },

    logout() {
        localStorage.clear();
        window.location.href = "/";
    },

    async request(url, options = {}) {
        const token = this.getToken();
        const headers = {
            "Content-Type": "application/json",
            ...options.headers
        };

        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }

        const response = await fetch(url, {
            ...options,
            headers
        });

        if (response.status === 401 || response.status === 403) {
            if (window.location.pathname !== "/" && window.location.pathname !== "/index.html") {
                this.logout();
                return null;
            }
        }

        if (response.status === 204) {
            return true;
        }

        const data = await response.json();
        if (!response.ok) {
            const errorMsg = data.validationErrors
                ? Object.values(data.validationErrors).join(", ")
                : data.error || "Operation failed";
            throw new Error(errorMsg);
        }

        return data;
    },

    get(url) {
        return this.request(url, { method: "GET" });
    },

    post(url, body) {
        return this.request(url, { method: "POST", body: JSON.stringify(body) });
    },

    patch(url, body) {
        return this.request(url, { method: "PATCH", body: body ? JSON.stringify(body) : undefined });
    },

    delete(url) {
        return this.request(url, { method: "DELETE" });
    }
};