// Global state
let dashboardData = null;
let currentChart = null;

// Initialize when page loads
document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
});

/**
 * Load dashboard data from JSON file
 */
async function loadDashboardData() {
    try {
        const response = await fetch('../output/dashboard-data.json');

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        dashboardData = await response.json();
        hideLoading();
        displayDashboard();
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showError(error.message);
    }
}

/**
 * Hide loading indicator and show content
 */
function hideLoading() {
    document.getElementById('loading').style.display = 'none';
    document.getElementById('content').style.display = 'block';
}

/**
 * Show error message
 */
function showError(message) {
    document.getElementById('loading').style.display = 'none';
    document.getElementById('error').style.display = 'block';
    document.getElementById('error-message').textContent = message;
}

/**
 * Display the dashboard with overall statistics
 */
function displayDashboard() {
    // Calculate total energy
    const totalEnergy = dashboardData.months.reduce((sum, month) => sum + month.totalMonthKwh, 0);

    // Update statistics
    document.getElementById('total-months').textContent = dashboardData.months.length;
    document.getElementById('total-energy').textContent = formatEnergy(totalEnergy);
    document.getElementById('last-update').textContent = formatDateTime(dashboardData.generatedAt);

    // Populate month selector
    const monthSelect = document.getElementById('month-select');
    dashboardData.months.forEach(month => {
        const option = document.createElement('option');
        option.value = `${month.year}-${month.month}`;
        option.textContent = month.label;
        monthSelect.appendChild(option);
    });

    // Add event listener for month selection
    monthSelect.addEventListener('change', (e) => {
        if (e.target.value) {
            const [year, month] = e.target.value.split('-').map(Number);
            displayMonthDetails(year, month);
        } else {
            document.getElementById('month-details').style.display = 'none';
        }
    });
}

/**
 * Display details for a specific month
 */
function displayMonthDetails(year, month) {
    const monthData = dashboardData.months.find(m => m.year === year && m.month === month);

    if (!monthData) {
        console.error('Month not found:', year, month);
        return;
    }

    // Show month details section
    document.getElementById('month-details').style.display = 'block';

    // Update month title
    document.getElementById('month-title').textContent = monthData.label;

    // Calculate statistics
    const daysWithData = monthData.days.filter(d => d.totalKwh > 0);
    const avgDaily = daysWithData.length > 0 ? monthData.totalMonthKwh / daysWithData.length : 0;
    const bestDay = monthData.days.reduce((best, day) =>
        day.totalKwh > best.totalKwh ? day : best
    , { totalKwh: 0, date: '' });

    // Update month statistics
    document.getElementById('month-total').textContent = formatEnergy(monthData.totalMonthKwh);
    document.getElementById('month-average').textContent = formatEnergy(avgDaily);
    document.getElementById('month-best').textContent =
        bestDay.totalKwh > 0 ? `${formatEnergy(bestDay.totalKwh)} (${formatDate(bestDay.date)})` : '-';

    // Update chart
    updateChart(monthData);

    // Update table
    updateTable(monthData);

    // Scroll to month details
    document.getElementById('month-details').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/**
 * Update the daily energy chart
 */
function updateChart(monthData) {
    const ctx = document.getElementById('daily-chart').getContext('2d');

    // Destroy existing chart if it exists
    if (currentChart) {
        currentChart.destroy();
    }

    // Prepare data
    const labels = monthData.days.map(day => formatDate(day.date));
    const data = monthData.days.map(day => day.totalKwh);

    // Create new chart
    currentChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Täglicher Ertrag (kWh)',
                data: data,
                backgroundColor: 'rgba(245, 158, 11, 0.6)',
                borderColor: 'rgba(245, 158, 11, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return formatEnergy(context.parsed.y);
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: 'Ertrag (kWh)'
                    },
                    ticks: {
                        callback: function(value) {
                            return value.toFixed(1) + ' kWh';
                        }
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: 'Datum'
                    }
                }
            }
        }
    });
}

/**
 * Update the daily details table
 */
function updateTable(monthData) {
    const tbody = document.querySelector('#daily-table tbody');
    tbody.innerHTML = '';

    monthData.days.forEach(day => {
        const row = document.createElement('tr');

        row.innerHTML = `
            <td>${formatDate(day.date)}</td>
            <td class="text-right"><strong>${formatEnergy(day.totalKwh)}</strong></td>
            <td class="text-right text-muted">${formatReading(day.firstReading)}</td>
            <td class="text-right text-muted">${formatReading(day.lastReading)}</td>
            <td class="text-right text-muted">${day.numMeasurements}</td>
        `;

        tbody.appendChild(row);
    });
}

/**
 * Format energy value with unit
 */
function formatEnergy(kwh) {
    return `${kwh.toFixed(2)} kWh`;
}

/**
 * Format reading value
 */
function formatReading(value) {
    if (value === null || value === undefined) {
        return '-';
    }
    return value.toFixed(3) + ' kWh';
}

/**
 * Format date string
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
}

/**
 * Format date-time string
 */
function formatDateTime(dateTimeString) {
    const date = new Date(dateTimeString);
    return date.toLocaleString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}
