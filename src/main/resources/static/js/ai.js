(function () {
    'use strict';

    function csrfConfig() {
        var meta = document.querySelector('meta[name="_csrf"]');
        var headerMeta = document.querySelector('meta[name="_csrf_header"]');
        return {
            name: (headerMeta && headerMeta.content) || 'X-CSRF-TOKEN',
            token: (meta && meta.content) || ''
        };
    }

    function aiPost(url, body) {
        var csrf = csrfConfig();
        var headers = { 'Content-Type': 'application/json' };
        if (csrf.token) {
            headers[csrf.name] = csrf.token;
        }
        return fetch(url, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(body || {})
        }).then(function (response) {
            if (!response.ok) {
                return response.json().then(function (err) {
                    throw new Error((err && err.message) || 'Request failed (' + response.status + ')');
                }).catch(function (e) {
                    if (e instanceof Error && e.message) {
                        throw e;
                    }
                    throw new Error('Request failed (' + response.status + ')');
                });
            }
            return response.json();
        });
    }

    function addBubble(text, fromBot) {
        var container = document.getElementById('foodbot-messages');
        if (!container) {
            return;
        }
        var wrap = document.createElement('div');
        wrap.className = 'flex ' + (fromBot ? 'justify-start' : 'justify-end');
        var bubble = document.createElement('div');
        bubble.className = fromBot
            ? 'bg-white border border-gray-200 text-gray-800 rounded-lg rounded-tl-none px-3 py-2 max-w-[80%] text-sm whitespace-pre-wrap'
            : 'bg-green-600 text-white rounded-lg rounded-tr-none px-3 py-2 max-w-[80%] text-sm whitespace-pre-wrap';
        bubble.textContent = text;
        wrap.appendChild(bubble);
        container.appendChild(wrap);
        container.scrollTop = container.scrollHeight;
    }

    function initFoodBot() {
        var toggle = document.getElementById('foodbot-toggle');
        var panel = document.getElementById('foodbot-panel');
        var closeBtn = document.getElementById('foodbot-close');
        var form = document.getElementById('foodbot-form');
        var input = document.getElementById('foodbot-input');
        var messages = document.getElementById('foodbot-messages');
        if (!toggle || !panel || !form) {
            return;
        }

        if (!messages.querySelector('.flex')) {
            addBubble("Hi! I'm FoodBot. I can help you understand how FoodShare works - how to register, post a donation, browse or accept donations, and the status flow. Ask me anything!", true);
        }

        toggle.addEventListener('click', function () {
            panel.hidden = !panel.hidden;
        });
        closeBtn.addEventListener('click', function () {
            panel.hidden = true;
        });

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            var message = (input.value || '').trim();
            if (!message) {
                return;
            }
            addBubble(message, false);
            input.value = '';
            addBubble('...', true);
            var typingBubble = messages.lastElementChild;
            aiPost('/ai/chat', { message: message })
                .then(function (data) {
                    typingBubble.textContent = (data && data.message) || 'No response.';
                })
                .catch(function (err) {
                    typingBubble.textContent = err.message;
                });
        });
    }

    document.addEventListener('DOMContentLoaded', initFoodBot);

    window.FoodShareAI = { post: aiPost };
})();
