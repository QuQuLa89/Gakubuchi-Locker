package com.gakubuchilocker.commands

import com.gakubuchilocker.GakubuchiLockerPlugin
import com.gakubuchilocker.database.DatabaseManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GakubuchiFinderCommandTest {
    private class Fixture {
        val plugin = mock(GakubuchiLockerPlugin::class.java)
        val db = mock(DatabaseManager::class.java)
        val scheduler = mock(BukkitScheduler::class.java)
        val command = mock(Command::class.java)
        val handler = GakubuchiFinderCommand(plugin)
        val runnables = mutableListOf<Runnable>()
        val tasks = mutableListOf<BukkitTask>()
        val frames = List(60) { frame() }

        init {
            `when`(plugin.db).thenReturn(db)
            `when`(db.isLocked(any(UUID::class.java) ?: UUID(0L, 0L))).thenReturn(true)
            `when`(scheduler.runTaskTimer(eq(plugin), any(Runnable::class.java), anyLong(), anyLong())).thenAnswer {
                runnables += it.getArgument<Runnable>(1)
                val task = mock(BukkitTask::class.java)
                `when`(task.taskId).thenReturn(tasks.size + 1)
                tasks += task
                task
            }
        }

        fun player(): Player =
            mock(Player::class.java).also {
                `when`(it.uniqueId).thenReturn(UUID.randomUUID())
                `when`(it.hasPermission("gakubuchilocker.use")).thenReturn(true)
                `when`(it.isOnline).thenReturn(true)
                `when`(it.isOp).thenReturn(true)
                `when`(it.getNearbyEntities(32.0, 32.0, 32.0)).thenReturn(frames)
            }

        private fun frame(): ItemFrame =
            mock(ItemFrame::class.java).also {
                `when`(it.uniqueId).thenReturn(UUID.randomUUID())
                `when`(it.isValid).thenReturn(true)
                `when`(it.location).thenReturn(Location(mock(World::class.java), 0.0, 64.0, 0.0))
            }

        fun execute(
            player: Player,
            label: String = "gakubuchifinder",
        ) {
            handler.onCommand(player, command, label, emptyArray())
        }

        @Suppress("DEPRECATION")
        fun quit(player: Player) {
            handler.onPlayerQuit(PlayerQuitEvent(player, ""))
        }
    }

    @Test
    fun `repeat command and alias replace only that players task and quit cleans it up`() {
        mockStatic(Bukkit::class.java).use { bukkit ->
            val f = Fixture()
            bukkit.`when`<BukkitScheduler> { Bukkit.getScheduler() }.thenReturn(f.scheduler)
            val first = f.player()
            val second = f.player()
            f.execute(first)
            f.execute(second)
            f.execute(first, "gfinder")
            assertEquals(3, f.tasks.size)
            verify(f.tasks[0]).cancel()
            verify(f.tasks[1], never()).cancel()
            f.quit(first)
            f.quit(first)
            verify(f.tasks[2], times(1)).cancel()
            f.quit(second)
            verify(f.tasks[1]).cancel()
        }
    }

    @Test
    fun `rendering is capped at fifty eligible frames and natural completion removes task`() {
        mockStatic(Bukkit::class.java).use { bukkit ->
            val f = Fixture()
            bukkit.`when`<BukkitScheduler> { Bukkit.getScheduler() }.thenReturn(f.scheduler)
            val player = f.player()
            f.execute(player)
            repeat(21) { f.runnables.single().run() }
            for (frame in f.frames.take(50)) verify(frame, times(20)).location
            for (frame in f.frames.drop(50)) verify(frame, never()).location
            verify(f.scheduler).cancelTask(1)
            f.quit(player)
            verify(f.tasks.single(), never()).cancel()
        }
    }

    @Test
    fun `empty search cancels previous rendering without scheduling another task`() {
        mockStatic(Bukkit::class.java).use { bukkit ->
            val f = Fixture()
            bukkit.`when`<BukkitScheduler> { Bukkit.getScheduler() }.thenReturn(f.scheduler)
            val player = f.player()
            f.execute(player)
            `when`(player.getNearbyEntities(32.0, 32.0, 32.0)).thenReturn(emptyList())
            f.execute(player)
            assertEquals(1, f.tasks.size)
            verify(f.tasks.single()).cancel()
            f.quit(player)
            verify(f.tasks.single(), times(1)).cancel()
        }
    }

    @Test
    fun `offline player stops rendering and releases task registration`() {
        mockStatic(Bukkit::class.java).use { bukkit ->
            val f = Fixture()
            bukkit.`when`<BukkitScheduler> { Bukkit.getScheduler() }.thenReturn(f.scheduler)
            val player = f.player()
            f.execute(player)
            `when`(player.isOnline).thenReturn(false)
            f.runnables.single().run()
            verify(f.scheduler).cancelTask(1)
            f.quit(player)
            verify(f.tasks.single(), never()).cancel()
        }
    }

    @Test
    fun `nonoperators only see owned frames and limit is applied after ownership filter`() {
        mockStatic(Bukkit::class.java).use { bukkit ->
            val f = Fixture()
            bukkit.`when`<BukkitScheduler> { Bukkit.getScheduler() }.thenReturn(f.scheduler)
            val player = f.player()
            `when`(player.isOp).thenReturn(false)
            val playerId = player.uniqueId
            `when`(f.db.getOwner(f.frames.last().uniqueId)).thenReturn(playerId)
            f.execute(player)
            f.runnables.single().run()
            verify(f.frames.last()).location
            for (frame in f.frames.dropLast(1)) verify(frame, never()).location
        }
    }

    @Test
    fun `plugin disable cancels all players tasks and releases registrations`() {
        mockStatic(Bukkit::class.java).use { bukkit ->
            val f = Fixture()
            bukkit.`when`<BukkitScheduler> { Bukkit.getScheduler() }.thenReturn(f.scheduler)
            val first = f.player()
            val second = f.player()
            f.execute(first)
            f.execute(second)
            f.handler.onPluginDisable(PluginDisableEvent(f.plugin))
            f.quit(first)
            f.quit(second)
            for (task in f.tasks) verify(task, times(1)).cancel()
        }
    }
}
