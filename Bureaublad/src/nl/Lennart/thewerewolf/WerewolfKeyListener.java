/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.Lennart.thewerewolf;

import org.getspout.spoutapi.event.input.InputListener;
import org.getspout.spoutapi.event.input.KeyPressedEvent;
import org.getspout.spoutapi.event.input.KeyReleasedEvent;

/**
 *
 * @author Lennart
 */
public class WerewolfKeyListener extends InputListener {

    public void onKeyPressedEvent(KeyPressedEvent e) {
        if (e.getKey().equals(e.getPlayer().getForwardKey())) {
            WerewolfEditor.setForwardDown(e.getPlayer(), true);
        }
    }

    public void onKeyReleasedEvent(KeyReleasedEvent e) {
        if (e.getKey().equals(e.getPlayer().getForwardKey())) {
            WerewolfEditor.setForwardDown(e.getPlayer(), false);
        }
    }

}
