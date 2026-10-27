package com.gakubuchilocker.listeners

import com.gakubuchilocker.GakubuchiLockerPlugin
import com.gakubuchilocker.database.DatabaseManager
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrameEventListenerTest {
    private class Fixture(
        val owner: Boolean = false,
        val op: Boolean = false,
    ) {
        val plugin = mock(GakubuchiLockerPlugin::class.java)
        val db = mock(DatabaseManager::class.java)
        val player = mock(Player::class.java)
        val frame = mock(ItemFrame::class.java)
        val modes = mutableMapOf<UUID, GakubuchiLockerPlugin.PendingMode>()
        val listener = FrameEventListener(plugin)
        val playerId = UUID.randomUUID()

        init {
            `when`(plugin.db).thenReturn(db)
            `when`(plugin.pendingMode).thenReturn(modes)
            `when`(player.uniqueId).thenReturn(playerId)
            `when`(player.isOp).thenReturn(op)
            `when`(frame.uniqueId).thenReturn(UUID.randomUUID())
            `when`(db.isLocked(frame.uniqueId)).thenReturn(true)
            `when`(db.getOwner(frame.uniqueId)).thenReturn(if (owner) playerId else UUID.randomUUID())
        }
    }

    @Test
    fun `offhand event does not repeat lock operations or messages`() {
        val f = Fixture()
        clearInvocations(f.player, f.frame)
        val event = PlayerInteractEntityEvent(f.player, f.frame, EquipmentSlot.OFF_HAND)
        f.listener.onPlayerInteractEntity(event)
        verifyNoInteractions(f.player, f.frame)
        verify(f.plugin, never()).pendingMode
        assertFalse(event.isCancelled)
    }

    @Test
    fun `unlock mode permits owners and operators and rejects other players`() {
        for (owner in listOf(false, true)) {
            for (op in listOf(false, true)) {
                val f = Fixture(owner, op)
                f.modes[f.playerId] = GakubuchiLockerPlugin.PendingMode.UNLOCK
                val event = PlayerInteractEntityEvent(f.player, f.frame, EquipmentSlot.HAND)
                f.listener.onPlayerInteractEntity(event)
                assertTrue(event.isCancelled)
                if (owner || op) verify(f.db).unlockFrame(f.frame) else verify(f.db, never()).unlockFrame(f.frame)
            }
        }
    }

    @Test
    fun `normal interactions permit only owners and operators on locked frames`() {
        for (owner in listOf(false, true)) {
            for (op in listOf(false, true)) {
                val f = Fixture(owner, op)
                val event = PlayerInteractEntityEvent(f.player, f.frame, EquipmentSlot.HAND)
                f.listener.onPlayerInteractEntity(event)
                assertEquals(!owner && !op, event.isCancelled)
                val atEvent = PlayerInteractAtEntityEvent(f.player, f.frame, Vector(), EquipmentSlot.HAND)
                f.listener.onPlayerInteractAtEntity(atEvent)
                assertEquals(!owner && !op, atEvent.isCancelled)
                verify(f.db, never()).unlockFrame(f.frame)
            }
        }
    }
}
