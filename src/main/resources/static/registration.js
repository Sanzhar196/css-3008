const form = document.getElementById('register');
const username = document.getElementById('newUsername');
const password = document.getElementById('newPassword');
const result = document.getElementById('registrationResult');
const submit = form.querySelector('button[type="submit"]');

form.addEventListener('submit', async event => {
  event.preventDefault();
  submit.disabled = true;
  result.hidden = true;

  try {
    const response = await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Correlation-Id': crypto.randomUUID(),
      },
      body: JSON.stringify({ username: username.value, password: password.value }),
    });
    const body = await response.json();
    const message = response.ok
      ? { ...body, message: 'Account created. Return to sign in with your new username.' }
      : body;
    result.textContent = JSON.stringify({ ...message, httpStatus: response.status }, null, 2);

    if (response.ok) {
      form.reset();
    }
  } catch {
    result.textContent = 'Could not complete registration. Check the connection and try again.';
  } finally {
    result.hidden = false;
    submit.disabled = false;
  }
});
