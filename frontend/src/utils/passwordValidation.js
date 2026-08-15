export const PASSWORD_COMPOSITION_REGEX = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).+$/;

export const validatePassword = (value) => {
  if (!value) return "비밀번호를 입력해주세요.";

  const lengthOk = value.length >= 8;
  const compositionOk = PASSWORD_COMPOSITION_REGEX.test(value);

  if (!lengthOk && !compositionOk) return "비밀번호는 8자 이상 입력해주세요. 영문, 숫자, 특수문자를 모두 포함해 입력해주세요.";
  if (!lengthOk) return "비밀번호는 8자 이상 입력해주세요.";
  if (!compositionOk) return "영문, 숫자, 특수문자를 모두 포함해 입력해주세요.";
  return "";
};

export const validatePasswordConfirm = (passwordValue, confirmValue) => {
  if (!passwordValue) return "";
  if (!confirmValue) return "비밀번호 확인을 입력해주세요.";
  if (passwordValue !== confirmValue) return "비밀번호가 일치하지 않습니다.";
  return "";
};
