package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.Theme;
import vn.edu.fpt.myfschool.common.enums.Language;

public record UserSettingDto(
    Theme theme,
    Language language,
    Boolean notificationEnabled
) {}
