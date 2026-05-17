package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.NoteEntity
import com.xpotrack.app.data.db.TaskEntity
import com.xpotrack.app.data.model.ReminderLevel

private val Silent = ReminderLevel.Silent.name
private val Notify = ReminderLevel.Notify.name
private val Alarm  = ReminderLevel.Alarm.name

// Mockup data seeded once on first launch so the screens still look like the design.

object SeedData {

    fun notes(now: Long): List<NoteEntity> {
        val day = 86_400_000L
        return listOf(
            NoteEntity(title = "Field notes — Sausalito",
                bodyMarkdown = "The ferry crosses at 7:40, then again at 9:15. Bring the linen jacket.",
                category = "Trip", isPinned = true,
                createdAt = now - 9 * day, updatedAt = now),
            NoteEntity(title = "Reading queue",
                bodyMarkdown = "· The Mezzanine — Nicholson Baker\n· Patrimony — Philip Roth\n· Cosmicomics",
                category = "Ideas",
                createdAt = now - 8 * day, updatedAt = now - 1 * day),
            NoteEntity(title = "On finishing things",
                bodyMarkdown = "A finished bad draft beats an unfinished perfect one. Show up before you feel ready.",
                category = "Essay",
                createdAt = now - 6 * day, updatedAt = now - 3 * day),
            NoteEntity(title = "Pasta dough",
                bodyMarkdown = "100g 00 flour · 1 egg · pinch salt · rest 30m wrapped",
                category = "Recipe",
                createdAt = now - 5 * day, updatedAt = now - 4 * day),
            NoteEntity(title = "Talk feedback — Anya",
                bodyMarkdown = "Slow down on slide 6. The point about leakage didn't land — try the metaphor first.",
                category = "Work",
                createdAt = now - 4 * day, updatedAt = now - 18 * day),
            NoteEntity(title = "Bike route, Marin loop",
                bodyMarkdown = "Sausalito → Tiburon ferry → Belvedere → climb to Ring Mtn → coast back via Strawberry",
                category = "Trip",
                createdAt = now - 3 * day, updatedAt = now - 22 * day),
            NoteEntity(title = "Therapy notes",
                bodyMarkdown = "Pattern: I retreat when projects get too public. Try shipping mid-confidence.",
                category = "Personal",
                createdAt = now - 2 * day, updatedAt = now - 24 * day),
            NoteEntity(title = "Q2 planning",
                bodyMarkdown = "Three bets: search refactor, mobile onboarding, ops dashboard. Pick two.",
                category = "Work", isPinned = true,
                createdAt = now - 1 * day, updatedAt = now - 26 * day),
            NoteEntity(title = "Shower thought",
                bodyMarkdown = "A todo list is just a note that yells at you.",
                category = "Inbox",
                createdAt = now, updatedAt = now - 28 * day),
        )
    }

    fun tasks(now: Long): List<TaskEntity> = listOf(
        TaskEntity(title = "Morning pages",          time = "07:00", level = Silent, durationMin = 30, isDone = true,  createdAt = now, updatedAt = now),
        TaskEntity(title = "Stretch + coffee",       time = "08:30", level = Silent, durationMin = 25, isDone = true,  createdAt = now, updatedAt = now),
        TaskEntity(title = "Stand-up — design crit", time = "09:15", level = Notify, durationMin = 30, createdAt = now, updatedAt = now),
        TaskEntity(title = "Review Anya's draft",    time = "11:00", level = Notify, durationMin = 60, createdAt = now, updatedAt = now),
        TaskEntity(title = "Lunch — Marin Sun",      time = "13:00", level = Silent, durationMin = 60, createdAt = now, updatedAt = now),
        TaskEntity(title = "Dentist · 18 Hawthorne", time = "14:30", level = Alarm,  durationMin = 45, createdAt = now, updatedAt = now),
        TaskEntity(title = "Call Dad",               time = "16:00", level = Alarm,  durationMin = 20, createdAt = now, updatedAt = now),
        TaskEntity(title = "Bike — Marin loop",      time = "17:30", level = Notify, durationMin = 90, createdAt = now, updatedAt = now),
        TaskEntity(title = "Read — Cosmicomics",     time = "20:30", level = Silent, durationMin = 45, createdAt = now, updatedAt = now),
    )
}
