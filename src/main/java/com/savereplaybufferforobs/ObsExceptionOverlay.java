package com.savereplaybufferforobs;

import lombok.NonNull;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;

import java.awt.*;
import java.util.Objects;

public class ObsExceptionOverlay extends OverlayPanel {
    private final SaveReplayBufferForObsConfig config;

    private final ObsException obsException;

    public ObsExceptionOverlay(SaveReplayBufferForObsConfig config, ObsException obsException) {
        this.config = config;
        this.obsException = obsException;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.displayErrorOverlay()) {
            return null;
        }
        final int padding = 10;
        final String displayText = obsException.getMessage();

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(LineComponent.builder()
                .left(displayText)
                .leftColor(Color.RED)
                .build());

        panelComponent.setPreferredSize(getTextWidth(graphics, displayText, padding));

        return panelComponent.render(graphics);
    }

    @NonNull
    private Dimension getTextWidth(Graphics2D graphics, String string, int offset) {
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int stringWidth = fontMetrics.stringWidth(string);
        return new Dimension(stringWidth + offset, 0);
    }

    public boolean isSameException(ObsException otherException) {
        return Objects.equals(otherException.getMessage(), obsException.getMessage());
    }
}
