/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.notification.plugin;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;

public class PostActivityPlugin extends AbstractNotificationPlugin {

  public PostActivityPlugin(InitParams initParams) {
    super(initParams);
  }

  public static final String ID = "PostActivityPlugin";
  
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public NotificationInfo makeNotification(NotificationContext ctx) {
    try {
      ExoSocialActivity activity = ctx.value(SocialNotificationUtils.ACTIVITY);

      return NotificationInfo.instance()
          .to(activity.getStreamOwner())
          .with(SocialNotificationUtils.POSTER.getKey(), Utils.getUserId(activity.getPosterId()))
          .with(SocialNotificationUtils.ACTIVITY_ID.getKey(), activity.getId())
          .key(getId()).end();
      
    } catch (Exception e) {
      ctx.setException(e);
    }
    
    return null;
  }

  @Override
  public MessageInfo makeMessage(NotificationContext ctx) {
    MessageInfo messageInfo = new MessageInfo();
    
    NotificationInfo notification = ctx.getNotificationInfo();
    
    String language = getLanguage(notification);
    TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
    SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

    String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
    ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
    Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
    
    
    templateContext.put("USER", identity.getProfile().getFullName());
    String subject = TemplateUtils.processSubject(templateContext);
    
    templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
    templateContext.put("ACTIVITY", activity.getTitle());
    templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity", activity.getId()));
    templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activity.getId()));
    String body = TemplateUtils.processGroovy(templateContext);
    
    return messageInfo.subject(subject).body(body).end();
  }

  @Override
  public boolean makeDigest(NotificationContext ctx, Writer writer) {
    List<NotificationInfo> notifications = ctx.getNotificationInfos();
    NotificationInfo first = notifications.get(0);
    String sendToUser = first.getTo();

    String language = getLanguage(first);
    
    TemplateContext templateContext = new TemplateContext(first.getKey().getId(), language);
    
    int count = notifications.size();
    String[] keys = {"USER", "USER_LIST", "LAST3_USERS"};
    String key = "";
    StringBuilder value = new StringBuilder();
    
    try {
      for (int i = 0; i < count && i < 3; i++) {
        String remoteId = notifications.get(i).getValueOwnerParameter(SocialNotificationUtils.POSTER.getKey());
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, true);
        //
        if (i > 1 && count == 3) {
          key = keys[i - 1];
        } else {
          key = keys[i];
        }
        value.append(SocialNotificationUtils.buildRedirecUrl("user", identity.getRemoteId(), identity.getProfile().getFullName()));
        if (count > (i + 1) && i < 2) {
          value.append(", ");
        }
      }
      templateContext.put(key, value.toString());
      if(count > 3) {
        templateContext.put("COUNT", SocialNotificationUtils.buildRedirecUrl("user_activity_stream", sendToUser, String.valueOf((count - 3))));
      }
      templateContext.put("USER_ACTIVITY_STREAM", LinkProviderUtils.getRedirectUrl("user_activity_stream", sendToUser));
      String digester = TemplateUtils.processDigest(templateContext.digestType(count));
      writer.append(digester);
      
    } catch (IOException e) {
      ctx.setException(e);
      return false;
    }
    return true;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    ExoSocialActivity activity = ctx.value(SocialNotificationUtils.ACTIVITY);
    if (activity.getStreamOwner().equals(Utils.getUserId(activity.getPosterId())) || Utils.isSpaceActivity(activity)) {
      return false;
    }
    return true;
  }

}
