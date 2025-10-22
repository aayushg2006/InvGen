document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = 'http://localhost:8080/api/auth';

    // --- LOGIN FORM ---
    if (document.getElementById('loginForm')) {
        document.getElementById('loginForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;

            try {
                const response = await fetch(`${API_BASE_URL}/login`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ username, password }),
                });

                if (!response.ok) {
                    throw new Error('Login failed! Please check your credentials.');
                }

                const data = await response.json();
                
                // Store the JWT token securely
                localStorage.setItem('accessToken', data.accessToken);

                alert('Login successful!');
                // Redirect to dashboard on successful login
                window.location.href = 'dashboard.html';

            } catch (error) {
                alert(error.message);
            }
        });
    }

    // --- REGISTRATION FORM ---
    if (document.getElementById('registerForm')) {
        document.getElementById('registerForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const registrationData = {
                fullName: document.getElementById('fullName').value,
                username: document.getElementById('username').value,
                password: document.getElementById('password').value,
                shopName: document.getElementById('shopName').value,
                shopGstin: document.getElementById('shopGstin').value,
            };

            try {
                const response = await fetch(`${API_BASE_URL}/register`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(registrationData),
                });

                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(`Registration failed: ${errorText}`);
                }
                
                alert('Registration successful! Please login.');
                // Redirect to login page
                window.location.href = 'index.html';

            } catch (error) {
                alert(error.message);
            }
        });
    }
});