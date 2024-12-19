const KEYWORDS = (
  "abstract boolean break byte case case catch char class const continue debugger default delete delete do " +
  "double else else enum export extends false final finally float for function goto if implements import in " +
  "instanceof int interface long native new new null package private protected public return return short static " +
  "super switch synchronized this throw throw throws transient true try typeof var void volatile while with " +
  "ndbx g"
).split(" ");

const TERMS = (
  "about access account accounts add address adm admin admin administration adult advertising affiliate affiliates " +
  "ajax analytics android anon anonymous api app apps archive atom auth authentication avatar backup banner " +
  "banners beta billing bin blog blog blogs board bot bots business cache cadastro calendar campaign careers " +
  "cgi chat client cliente code code comercial compare compras config connect contact contest create css dashboard " +
  "data db delete demo design design designer dev dev devel dir directory doc docs domain download downloads " +
  "ecommerce edit editor email faq favorite feed feedback file files flog follow forum forums free ftp ftp gadget " +
  "gadgets games group groups guest help home homepage host hosting hostname hpg html http httpd https image " +
  "images imap imap img index indice info information intranet invite ipad iphone irc java javascript job jobs " +
  "js knowledgebase list lists log login logout logs mail mail mail1 mail2 mail3 mail4 mail5 mailer mailing " +
  "manager marketing master me media message messenger microblog microblogs mine mob mobile movie movies mp3 " +
  "msg msn music musicas mx my mysql name named net network new news newsletter nick nickname notes noticias " +
  "ns ns1 ns10 ns2 ns3 ns4 ns5 ns6 ns7 ns8 ns9 old online operator order orders page pager pages panel password " +
  "perl photo photoalbum photos php pic pics plugin plugins pop pop pop3 pop3 post postfix postmaster posts " +
  "profile project projects promo pub public python random register registration root rss ruby sale sales sample " +
  "samples script scripts search secure security send service setting settings setup shop signin signup site " +
  "sitemap sites smtp smtp soporte sql ssh stage stage staging start stat static stats stats status status store " +
  "stores student subdomain subscribe suporte support system tablet tablets talk task tasks tech telnet test test1 test2 " +
  "test3 teste tests theme themes tmp todo tools tv update upload url usage user username usuario vendas video " +
  "videos visitor web webmail webadmin webmaster website websites win workshop ww wws www www www1 www2 www3 www4 www5 " +
  "www6 www7 wwws wwww xpg xxx you yourdomain yourname yoursite yourusername"
).split(" ");

const RESERVED_WORDS = KEYWORDS.concat(TERMS);

// Check the username. Throw an error if invalid. Otherwise, return the username.
export function validateUsername(login) {
  if (!/^[\w\d][\w\d\-]{2,30}$/.test(login)) {
    throw new Error("Username may only contain letters, digits or dashes and cannot begin with a dash.");
  } else if (RESERVED_WORDS.includes(login.toLowerCase())) {
    throw new Error("Username cannot be a reserved word.");
  } else {
    return login;
  }
}

// Check the email address. Throw an error if invalid. Otherwise, return the email.
export function validateEmail(email) {
  if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i.test(email)) {
    throw new Error("Email address is invalid.");
  } else {
    return email;
  }
}

// Check the password. Throw an error if invalid. Otherwise, return the password.
export function validatePassword(password) {
  if (password.trim().length === 0) {
    throw new Error("Password is too short.");
  } else if (password.length < 6 || password.length > 1000) {
    throw new Error("Password is too short.");
  } else {
    return password;
  }
}

// Check the slug, the short URL used for page names etc.
export function validateSlug(slug) {
  if (!/^[a-z0-9\-]{1,100}$/.test(slug)) {
    throw new Error("Invalid slug.");
  } else {
    return slug;
  }
}

// Check a version number and throw an error if invalid. Otherwise, return the version number.
// Version numbers should be in the format x.y.z, where x, y and z are integers.
// Valid version numbers are "0.0.1", "9.8.7" and "1.11.456".
export function validateVersion(v) {
  if (!/^[0-9]{1,4}\.[0-9]{1,4}\.[0-9]{1,4}$/.test(v)) {
    throw new Error('Invalid version number. (Use three numbers, e.g. "1.0.7")');
  } else {
    return v;
  }
}

// Check the project name and throw an error if invalid. Otherwise, return the project name.
// Project name need to start with a letter and can contain only letters and digits.
// They need to be between three and sixteen characters long.
// It can't be a reserved keyword.
export function validateProjectId(s) {
  if (!/^[a-z][a-zA-Z0-9]{2,15}$/.test(s)) {
    throw new Error("Project name needs to start with a letter and contain only letters or digits.");
  } else if (KEYWORDS.includes(s)) {
    throw new Error("Project name cannot be a reserved keyword.");
  } else {
    return s;
  }
}
