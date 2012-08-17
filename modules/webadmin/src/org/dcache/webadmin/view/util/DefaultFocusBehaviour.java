package org.dcache.webadmin.view.util;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.FormComponent;

/**
 * This Behaviour may be added to a component that is a child of a Form. It
 * makes the component the one which has the focus when the page is displayed by
 * adding the corresponding javascript to it.
 * should be given only to one of the children(because only one can have the
 * focus naturally).
 * @author jans
 */
public class DefaultFocusBehaviour extends AbstractBehavior {

    private static final long serialVersionUID = -4891399118136854774L;
    private Component component;

    @Override
    public void bind(Component component) {
        if (!(component instanceof FormComponent)) {
            throw new IllegalArgumentException(
                    "DefaultFocusBehavior: component must be instanceof FormComponent");
        }
        this.component = component;
        component.setOutputMarkupId(true);
    }

    @Override
    public void renderHead(IHeaderResponse iHeaderResponse) {
        super.renderHead(iHeaderResponse);
        iHeaderResponse.renderOnLoadJavascript(
                "document.getElementById('" + component.getMarkupId() +
                "').focus();");
    }
}

