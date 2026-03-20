/**
 * GuideConnect - Main JavaScript
 * Interactive features for the tour booking platform.
 */
document.addEventListener('DOMContentLoaded', function () {

    // ---- Star Rating Selector ----
    initStarRatings();

    // ---- Character Counters ----
    initCharCounters();

    // ---- Price Calculator ----
    initPriceCalculator();

    // ---- Confirm Dialogs ----
    initConfirmDialogs();

    // ---- Chat Auto-Scroll ----
    initChatAutoScroll();

    // ---- Form Validation Feedback ----
    initFormValidation();

    // ---- Toast Notifications ----
    initToasts();
});

/* ============================================
   Star Rating Selector
   ============================================ */
function initStarRatings() {
    document.querySelectorAll('.star-rating-input').forEach(function (container) {
        var labels = container.querySelectorAll('label');
        var inputs = container.querySelectorAll('input[type="radio"]');
        var hiddenInput = container.closest('form')
            ? container.closest('form').querySelector('input[name="starRating"]')
            : null;

        labels.forEach(function (label) {
            label.addEventListener('click', function () {
                var value = label.getAttribute('data-value') || label.htmlFor;
                var radio = document.getElementById(label.htmlFor);
                if (radio) {
                    radio.checked = true;
                }

                // Update visual state
                labels.forEach(function (l) {
                    var lValue = l.getAttribute('data-value') || '';
                    var rId = l.htmlFor;
                    var r = document.getElementById(rId);
                    if (r && parseInt(r.value) <= parseInt(radio.value)) {
                        l.classList.add('selected');
                        l.style.color = '#F1C40F';
                    } else {
                        l.classList.remove('selected');
                        l.style.color = '#D5D8DC';
                    }
                });

                // Update hidden input if present
                if (hiddenInput && radio) {
                    hiddenInput.value = radio.value;
                }
            });
        });

        // Also handle direct radio input changes
        inputs.forEach(function (input) {
            input.addEventListener('change', function () {
                var selectedVal = parseInt(input.value);
                labels.forEach(function (l) {
                    var r = document.getElementById(l.htmlFor);
                    if (r && parseInt(r.value) <= selectedVal) {
                        l.style.color = '#F1C40F';
                    } else {
                        l.style.color = '#D5D8DC';
                    }
                });
            });
        });
    });
}

/* ============================================
   Character Counter for Textareas
   ============================================ */
function initCharCounters() {
    document.querySelectorAll('textarea[data-max-chars], textarea[maxlength]').forEach(function (textarea) {
        var maxChars = parseInt(textarea.getAttribute('data-max-chars') || textarea.getAttribute('maxlength') || '500');
        var counterId = textarea.id + '-counter';

        // Create counter element if it doesn't exist
        var counter = document.getElementById(counterId);
        if (!counter) {
            counter = document.createElement('div');
            counter.id = counterId;
            counter.className = 'char-counter';
            textarea.parentNode.insertBefore(counter, textarea.nextSibling);
        }

        function updateCounter() {
            var remaining = maxChars - textarea.value.length;
            counter.textContent = textarea.value.length + ' / ' + maxChars + ' characters';

            if (remaining < 0) {
                counter.classList.add('over-limit');
                counter.textContent += ' (over limit!)';
            } else if (remaining < 50) {
                counter.classList.add('over-limit');
                counter.classList.remove('over-limit');
                counter.style.color = '#E67E22';
            } else {
                counter.classList.remove('over-limit');
                counter.style.color = '';
            }
        }

        textarea.addEventListener('input', updateCounter);
        updateCounter();
    });
}

/* ============================================
   Price Calculator
   ============================================ */
function initPriceCalculator() {
    var groupSizeInput = document.getElementById('groupSize');
    var pricePerPersonEl = document.getElementById('pricePerPerson');
    var subtotalEl = document.getElementById('subtotal');
    var platformFeeEl = document.getElementById('platformFee');
    var totalPriceEl = document.getElementById('totalPrice');
    var totalPriceInput = document.getElementById('totalPriceInput');

    if (!groupSizeInput || !pricePerPersonEl) return;

    function calculatePrice() {
        var groupSize = parseInt(groupSizeInput.value) || 1;
        var pricePerPerson = parseFloat(pricePerPersonEl.textContent || pricePerPersonEl.value || pricePerPersonEl.getAttribute('data-price')) || 0;

        var subtotal = groupSize * pricePerPerson;
        var fee = subtotal * 0.10;
        var total = subtotal + fee;

        if (subtotalEl) subtotalEl.textContent = '$' + subtotal.toFixed(2);
        if (platformFeeEl) platformFeeEl.textContent = '$' + fee.toFixed(2);
        if (totalPriceEl) totalPriceEl.textContent = '$' + total.toFixed(2);
        if (totalPriceInput) totalPriceInput.value = total.toFixed(2);
    }

    groupSizeInput.addEventListener('input', calculatePrice);
    groupSizeInput.addEventListener('change', calculatePrice);
    calculatePrice();
}

/* ============================================
   Confirm Dialogs for Destructive Actions
   ============================================ */
function initConfirmDialogs() {
    // Cancel booking buttons
    document.querySelectorAll('[data-confirm]').forEach(function (element) {
        element.addEventListener('click', function (e) {
            var message = element.getAttribute('data-confirm') || 'Are you sure you want to proceed?';
            if (!confirm(message)) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
        });
    });

    // Specific action buttons
    document.querySelectorAll('.btn-cancel-booking').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            if (!confirm('Are you sure you want to cancel this booking? This action cannot be undone.')) {
                e.preventDefault();
            }
        });
    });

    document.querySelectorAll('.btn-delete-tour').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            if (!confirm('Are you sure you want to delete this tour listing? All associated data will be affected.')) {
                e.preventDefault();
            }
        });
    });

    document.querySelectorAll('.btn-ban-user').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            if (!confirm('Are you sure you want to ban this user? They will lose access to their account.')) {
                e.preventDefault();
            }
        });
    });

    document.querySelectorAll('.btn-reject-booking').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            if (!confirm('Are you sure you want to reject this booking request?')) {
                e.preventDefault();
            }
        });
    });
}

/* ============================================
   Chat Auto-Scroll
   ============================================ */
function initChatAutoScroll() {
    var chatContainer = document.querySelector('.chat-container');
    if (chatContainer) {
        scrollToBottom(chatContainer);

        // Observe for new messages added dynamically
        var observer = new MutationObserver(function () {
            scrollToBottom(chatContainer);
        });
        observer.observe(chatContainer, { childList: true, subtree: true });
    }
}

function scrollToBottom(container) {
    container.scrollTop = container.scrollHeight;
}

/* ============================================
   Form Validation Feedback
   ============================================ */
function initFormValidation() {
    document.querySelectorAll('form[data-validate]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!form.checkValidity()) {
                e.preventDefault();
                e.stopPropagation();
            }
            form.classList.add('was-validated');
        });

        // Real-time validation on blur
        form.querySelectorAll('input, textarea, select').forEach(function (field) {
            field.addEventListener('blur', function () {
                if (form.classList.contains('was-validated')) {
                    validateField(field);
                }
            });

            field.addEventListener('input', function () {
                if (field.classList.contains('is-invalid')) {
                    validateField(field);
                }
            });
        });
    });

    // Email validation
    document.querySelectorAll('input[type="email"]').forEach(function (input) {
        input.addEventListener('blur', function () {
            if (input.value && !isValidEmail(input.value)) {
                input.classList.add('is-invalid');
                setFieldError(input, 'Please enter a valid email address.');
            } else {
                input.classList.remove('is-invalid');
            }
        });
    });

    // Password match validation
    var confirmPasswordInput = document.getElementById('confirmPassword');
    var passwordInput = document.getElementById('password');
    if (confirmPasswordInput && passwordInput) {
        confirmPasswordInput.addEventListener('input', function () {
            if (confirmPasswordInput.value !== passwordInput.value) {
                confirmPasswordInput.classList.add('is-invalid');
                setFieldError(confirmPasswordInput, 'Passwords do not match.');
            } else {
                confirmPasswordInput.classList.remove('is-invalid');
                confirmPasswordInput.classList.add('is-valid');
            }
        });
    }
}

function validateField(field) {
    if (field.checkValidity()) {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
    } else {
        field.classList.remove('is-valid');
        field.classList.add('is-invalid');
    }
}

function setFieldError(field, message) {
    var feedback = field.parentNode.querySelector('.invalid-feedback');
    if (!feedback) {
        feedback = document.createElement('div');
        feedback.className = 'invalid-feedback';
        field.parentNode.appendChild(feedback);
    }
    feedback.textContent = message;
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/* ============================================
   Bootstrap Toast Notifications
   ============================================ */
function initToasts() {
    // Initialize any existing toasts
    document.querySelectorAll('.toast').forEach(function (toastEl) {
        var toast = new bootstrap.Toast(toastEl, {
            autohide: true,
            delay: 5000
        });
        // Auto-show toasts with data-auto-show attribute
        if (toastEl.hasAttribute('data-auto-show')) {
            toast.show();
        }
    });
}

/**
 * Show a toast notification programmatically.
 * @param {string} message - The notification message.
 * @param {string} type - 'success', 'danger', 'warning', or 'info'.
 */
function showToast(message, type) {
    type = type || 'info';

    var iconMap = {
        success: 'bi-check-circle-fill',
        danger: 'bi-exclamation-triangle-fill',
        warning: 'bi-exclamation-circle-fill',
        info: 'bi-info-circle-fill'
    };

    var container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        container.style.zIndex = '1090';
        document.body.appendChild(container);
    }

    var toastId = 'toast-' + Date.now();
    var html = '<div id="' + toastId + '" class="toast align-items-center text-bg-' + type + ' border-0" role="alert" aria-live="assertive" aria-atomic="true">'
        + '<div class="d-flex">'
        + '<div class="toast-body">'
        + '<i class="bi ' + (iconMap[type] || iconMap.info) + ' me-2"></i>'
        + message
        + '</div>'
        + '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>'
        + '</div>'
        + '</div>';

    container.insertAdjacentHTML('beforeend', html);

    var toastEl = document.getElementById(toastId);
    var toast = new bootstrap.Toast(toastEl, { autohide: true, delay: 5000 });
    toast.show();

    // Remove from DOM after hidden
    toastEl.addEventListener('hidden.bs.toast', function () {
        toastEl.remove();
    });
}
