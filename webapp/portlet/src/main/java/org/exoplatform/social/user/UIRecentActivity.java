package org.exoplatform.social.user;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.user.portlet.UIRecentActivitiesPortlet;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

import java.util.Locale;

@ComponentConfig(
   template = "war:/groovy/social/webui/profile/UIRecentActivity.gtmpl"
)
public class UIRecentActivity extends UIContainer {
  public static String COMPONENT_ID = "Activity";
  private ExoSocialActivity activity = null;

  public UIRecentActivity() {
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);
    ((UIRecentActivitiesPortlet) getParent()).initProfilePopup();
    ((UIRecentActivitiesPortlet) getParent()).initSpacePopup();
  }

  public static String buildComponentId(String activityId) {
    return COMPONENT_ID + activityId;
  }

  protected ExoSocialActivity getActivity() {
    if (activity.getTitleId() != null) {
      WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
      I18NActivityProcessor i18NActivityProcessor = getApplicationComponent(I18NActivityProcessor.class);
      Locale userLocale = requestContext.getLocale();
      activity = i18NActivityProcessor.processKeys(activity, userLocale);
    }
    return this.activity;
  }

  public void setActivity(ExoSocialActivity activity) {
    this.activity = activity;
  }
}
