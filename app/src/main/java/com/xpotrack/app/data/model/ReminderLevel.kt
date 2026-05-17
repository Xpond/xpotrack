package com.xpotrack.app.data.model

// Domain enum — no UI dependencies. Persisted to DB as the enum name string.
enum class ReminderLevel { Silent, Notify, Alarm }
