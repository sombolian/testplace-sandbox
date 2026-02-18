// ===== DOM ELEMENTS =====
const amountInput = document.getElementById('amountInput');
const output = document.getElementById('output');
const outputSecondary = document.getElementById('outputSecondary');
const displayLabel = document.getElementById('displayLabel');
const feeToggle = document.getElementById('feeToggle');
const feeStatus = document.getElementById('feeStatus');
const currencyButton = document.getElementById('currencyButton');
const currencyModal = document.getElementById('currencyModal');
const currencySearch = document.getElementById('currencySearch');
const currencyList = document.getElementById('currencyList');
const selectedCurrencyText = document.getElementById('selectedCurrencyText');
const dropdownArrow = document.getElementById('dropdownArrow');
const closeCurrencyModal = document.getElementById('closeCurrencyModal');
const toggleButton = document.getElementById('toggleButton');
const toggleFrom = document.getElementById('toggleFrom');
const toggleTo = document.getElementById('toggleTo');
const inputIcon = document.getElementById('inputIcon');

// Memorial elements
const memorialButton = document.getElementById('memorialButton');
const memorialModal = document.getElementById('memorialModal');
const closeMemorialModal = document.getElementById('closeMemorialModal');

// ===== STATE =====
let isRobuxToUSD = true;
let exchangeRates = {};
let selectedCurrency = '';
let currencySymbols = {
    "USD": "$", "AED": "د.إ", "AFN": "؋", "ALL": "L", "AMD": "֏", "ANG": "ƒ",
    "AOA": "Kz", "ARS": "$", "AUD": "$", "AWG": "ƒ", "AZN": "₼", "BAM": "KM",
    "BBD": "$", "BDT": "৳", "BGN": "лв", "BHD": ".د.ب", "BIF": "FBu", "BMD": "$",
    "BND": "$", "BOB": "Bs.", "BRL": "R$", "BSD": "$", "BTN": "Nu.", "BWP": "P",
    "BYN": "Br", "BZD": "BZ$", "CAD": "$", "CDF": "FC", "CHF": "CHF", "CLP": "$",
    "CNY": "¥", "COP": "$", "CRC": "₡", "CUP": "₱", "CVE": "Esc", "CZK": "Kč",
    "DJF": "Fdj", "DKK": "kr", "DOP": "RD$", "DZD": "د.ج", "EGP": "£", "ERN": "Nfk",
    "ETB": "Br", "EUR": "€", "FJD": "$", "FKP": "£", "FOK": "kr", "GBP": "£",
    "GEL": "₾", "GGP": "£", "GHS": "₵", "GIP": "£", "GMD": "D", "GNF": "FG",
    "GTQ": "Q", "GYD": "$", "HKD": "$", "HNL": "L", "HRK": "kn", "HTG": "G",
    "HUF": "Ft", "IDR": "Rp", "ILS": "₪", "IMP": "£", "INR": "₹", "IQD": "ع.د",
    "IRR": "﷼", "ISK": "kr", "JEP": "£", "JMD": "J$", "JOD": "د.ا", "JPY": "¥",
    "KES": "KSh", "KGS": "с", "KHR": "៛", "KID": "$", "KMF": "CF", "KRW": "₩",
    "KWD": "د.ك", "KYD": "$", "KZT": "₸", "LAK": "₭", "LBP": "ل.ل", "LKR": "₨",
    "LRD": "$", "LSL": "L", "LYD": "ل.د", "MAD": "د.م.", "MDL": "L", "MGA": "Ar",
    "MKD": "ден", "MMK": "K", "MNT": "₮", "MOP": "MOP$", "MRU": "UM", "MUR": "₨",
    "MVR": "Rf", "MWK": "MK", "MXN": "$", "MYR": "RM", "MZN": "MT", "NAD": "$",
    "NGN": "₦", "NIO": "C$", "NOK": "kr", "NPR": "₨", "NZD": "$", "OMR": "ر.ع.",
    "PAB": "B/.", "PEN": "S/", "PGK": "K", "PHP": "₱", "PKR": "₨", "PLN": "zł",
    "PYG": "₲", "QAR": "ر.ق", "RON": "lei", "RSD": "дін.", "RUB": "₽", "RWF": "FRw",
    "SAR": "ر.س", "SBD": "$", "SCR": "₨", "SDG": "ج.س.", "SEK": "kr", "SGD": "$",
    "SHP": "£", "SLE": "Le", "SLL": "Le", "SOS": "Sh", "SRD": "$", "SSP": "£",
    "STN": "Db", "SYP": "£", "SZL": "E", "THB": "฿", "TJS": "SM", "TMT": "T",
    "TND": "د.ت", "TOP": "T$", "TRY": "₺", "TTD": "TT$", "TVD": "$", "TWD": "NT$",
    "TZS": "TSh", "UAH": "₴", "UGX": "USh", "UYU": "$U", "UZS": "so'm", "VES": "Bs.S",
    "VND": "₫", "VUV": "VT", "WST": "WS$", "XAF": "FCFA", "XCD": "EC$", "XDR": "XDR",
    "XOF": "CFA", "XPF": "CFPF", "YER": "﷼", "ZAR": "R", "ZMW": "ZK", "ZWL": "Z$"
};

// ===== EXCHANGE RATE FETCH =====
fetch('https://v6.exchangerate-api.com/v6/50bc16784a31e6c31284eed5/latest/USD')
    .then(response => response.json())
    .then(data => {
        exchangeRates = data.conversion_rates;
        initializeCurrencyDropdown();
        calculate();
        loadCurrency();
    })
    .catch(error => {
        console.error('Error fetching exchange rates:', error);
    });

// ===== CURRENCY DROPDOWN =====
function initializeCurrencyDropdown() {
    const currencies = Object.keys(exchangeRates).filter(c => c !== 'USD');
    currencyList.innerHTML = '';

    currencies.forEach(currency => {
        const item = document.createElement('div');
        item.className = 'currency-item';
        item.innerHTML = `
            <span class="currency-symbol">${currencySymbols[currency] || '?'}</span>
            <span class="currency-code">${currency}</span>
            <span class="currency-rate">${exchangeRates[currency].toFixed(4)}</span>
        `;
        item.addEventListener('click', () => selectCurrency(currency));
        currencyList.appendChild(item);
    });
}

function selectCurrency(currency) {
    selectedCurrency = currency;
    selectedCurrencyText.textContent = `${currencySymbols[currency]} ${currency}`;
    selectedCurrencyText.classList.add('active');
    currencyModal.classList.add('hidden');
    dropdownArrow.classList.remove('rotated');
    saveCurrency();
    calculate();
}

// ===== CURRENCY MODAL =====
currencyButton.addEventListener('click', (e) => {
    e.stopPropagation();
    currencyModal.classList.remove('hidden');
    dropdownArrow.classList.add('rotated');
    setTimeout(() => currencySearch.focus(), 150);
});

currencySearch.addEventListener('input', (e) => {
    const filter = e.target.value.toLowerCase();
    const items = currencyList.getElementsByClassName('currency-item');
    for (let i = 0; i < items.length; i++) {
        const code = items[i].querySelector('.currency-code').textContent.toLowerCase();
        const sym = items[i].querySelector('.currency-symbol').textContent;
        items[i].style.display = (code.includes(filter) || sym.includes(filter)) ? 'flex' : 'none';
    }
});

closeCurrencyModal.addEventListener('click', () => {
    currencyModal.classList.add('hidden');
    dropdownArrow.classList.remove('rotated');
});

currencyModal.addEventListener('click', (e) => {
    if (e.target === currencyModal || e.target.classList.contains('modal-backdrop')) {
        currencyModal.classList.add('hidden');
        dropdownArrow.classList.remove('rotated');
    }
});

currencyModal.querySelector('.modal-sheet').addEventListener('click', (e) => {
    e.stopPropagation();
});

// ===== DIRECTION TOGGLE =====
toggleButton.addEventListener('click', () => {
    isRobuxToUSD = !isRobuxToUSD;
    updateToggleUI();
    calculate();
});

function updateToggleUI() {
    if (isRobuxToUSD) {
        toggleFrom.innerHTML = '<img src="robuxicon.svg" alt="R$" class="toggle-icon"><span>Robux</span>';
        toggleTo.innerHTML = '<span>USD</span><span class="toggle-dollar">$</span>';
        inputIcon.innerHTML = '<img src="robuxicon.svg" alt="R$" class="robux-input-icon">';
        displayLabel.textContent = 'ROBUX TO USD';
    } else {
        toggleFrom.innerHTML = '<span class="toggle-dollar">$</span><span>USD</span>';
        toggleTo.innerHTML = '<img src="robuxicon.svg" alt="R$" class="toggle-icon"><span>Robux</span>';
        inputIcon.innerHTML = '<span class="dollar-sign">$</span>';
        displayLabel.textContent = 'USD TO ROBUX';
    }
}

// ===== INPUT & CALCULATION =====
amountInput.addEventListener('input', () => {
    calculate();
    output.classList.add('animate-pop');
    output.addEventListener('animationend', () => {
        output.classList.remove('animate-pop');
    }, { once: true });
});

function calculate() {
    const amount = amountInput.value.replace(/,/g, '').trim();
    try {
        let calculationResult = amount === '' ? 0 : math.evaluate(amount);
        let value = calculationResult || 0;

        if (isRobuxToUSD) {
            let outputUSD = value * 0.0038 * (feeToggle.checked ? 0.7 : 1);
            output.textContent = `${currencySymbols['USD']}${formatNumber(outputUSD)}`;

            if (selectedCurrency && exchangeRates[selectedCurrency]) {
                const converted = outputUSD * exchangeRates[selectedCurrency];
                outputSecondary.textContent = `${currencySymbols[selectedCurrency]}${formatNumber(converted)} ${selectedCurrency}`;
            } else {
                outputSecondary.textContent = '';
            }
        } else {
            const robuxAmount = Math.floor(value / 0.0038 / (feeToggle.checked ? 0.7 : 1));
            output.textContent = `R$ ${formatNumber(robuxAmount)}`;
            outputSecondary.textContent = '';
        }
    } catch (error) {
        output.textContent = isRobuxToUSD ? '$0.00' : 'R$ 0';
        outputSecondary.textContent = '';
    }
}

function formatNumber(number) {
    return number.toLocaleString('en-US', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    });
}

// ===== FEE TOGGLE =====
feeToggle.addEventListener('change', () => {
    feeStatus.textContent = feeToggle.checked ? 'ON' : 'OFF';
    feeStatus.classList.toggle('active', feeToggle.checked);
    calculate();
});

// ===== MEMORIAL MODAL =====
memorialButton.addEventListener('click', (e) => {
    e.stopPropagation();
    memorialModal.classList.remove('hidden');
});

closeMemorialModal.addEventListener('click', () => {
    memorialModal.classList.add('hidden');
});

memorialModal.addEventListener('click', (e) => {
    if (e.target === memorialModal || e.target.classList.contains('modal-backdrop')) {
        memorialModal.classList.add('hidden');
    }
});

memorialModal.querySelector('.modal-sheet').addEventListener('click', (e) => {
    e.stopPropagation();
});

// ===== KEYBOARD =====
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        if (!currencyModal.classList.contains('hidden')) {
            currencyModal.classList.add('hidden');
            dropdownArrow.classList.remove('rotated');
        }
        if (!memorialModal.classList.contains('hidden')) {
            memorialModal.classList.add('hidden');
        }
    }
});

// ===== COOKIE HELPERS =====
function setCookie(name, value, days) {
    const date = new Date();
    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
    document.cookie = name + "=" + value + ";expires=" + date.toUTCString() + ";path=/";
}

function getCookie(name) {
    const cookieName = name + "=";
    const decoded = decodeURIComponent(document.cookie);
    const parts = decoded.split(';');
    for (let i = 0; i < parts.length; i++) {
        let c = parts[i].trim();
        if (c.indexOf(cookieName) === 0) {
            return c.substring(cookieName.length);
        }
    }
    return "";
}

function saveCurrency() {
    setCookie('selectedCurrency', selectedCurrency, 365);
}

function loadCurrency() {
    const saved = getCookie('selectedCurrency');
    if (saved && exchangeRates[saved]) {
        selectCurrency(saved);
    } else {
        selectedCurrencyText.textContent = 'USD';
        selectedCurrencyText.classList.remove('active');
        selectedCurrency = '';
    }
}

// ===== INIT =====
calculate();
