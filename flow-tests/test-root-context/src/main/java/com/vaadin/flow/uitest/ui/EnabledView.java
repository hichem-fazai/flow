/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.EnabledView", layout = ViewTestLayout.class)
public class EnabledView extends Div {

    public EnabledView() {
        setId("main");

        Div div = new Div();
        div.setText("Target to enable/disable");
        div.setId("enabled");
        div.getElement().setEnabled(false);

        Label label = new Label("Nested element");
        label.setId("nested-label");
        div.add(label);

        NativeButton updateStyle = new NativeButton(
                "Update target element property", event -> {
                    div.setClassName("foo");
                    label.setClassName("bar");
                });
        updateStyle.setId("updateProperty");
        updateStyle.getElement().setEnabled(false);
        updateStyle.setClassName("disabled");

        NativeButton updateEnableButton = new NativeButton(
                "Change enable state for buttons", event -> {
                    updateStyle.getElement()
                            .setEnabled(!updateStyle.getElement().isEnabled());
                    updateStyle.setClassName("disabled",
                            !updateStyle.getElement().isEnabled());
                });
        updateEnableButton.setId("enableButton");

        add(div, updateStyle, updateEnableButton);
    }

}