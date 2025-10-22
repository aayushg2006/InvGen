const API_BASE_URL = 'http://localhost:8080/api';

// Helper function to get the JWT token from localStorage
function getToken() {
    return localStorage.getItem('accessToken');
}

// Helper function to make authenticated API requests
async function fetchWithAuth(endpoint, options = {}) {
    const token = getToken();

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
    });

    if (response.status === 401 || response.status === 403) {
        // Handle unauthorized access by redirecting to the login page
        alert('Your session has expired. Please log in again.');
        window.location.href = 'index.html';
        throw new Error('Unauthorized');
    }
    
    // Get the response text to include in the error message if needed
    const responseText = await response.text();

    if (!response.ok) {
        throw new Error(`API request failed: ${responseText}`);
    }

    try {
        // Try to parse as JSON, but return text if it fails
        return JSON.parse(responseText);
    } catch (e) {
        return responseText;
    }
}