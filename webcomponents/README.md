# Sakai Web Components

## Why?

Sakai is very permissive about how tools are written. They can be written in any Java web framework you choose just as
long as you follow a few guidelines. This has proven to be very empowering for the Sakai community - it has opened the
door to developers from differing development backgrounds and let them do things their way. On the other hand, this
approach has made it more difficult to standardise the user experience across Sakai and to share UI code. This is an
area where web components shine. If we can extract common chunks of functionality into shared components, we can gradually
make the Sakai codebase smaller and more maintainable.

## What are Web Components?

First, a few links:

[W3C](https://www.w3.org/wiki/WebComponents/)

[MDN](https://developer.mozilla.org/en-US/docs/Web/Web\_Components)

[Lit-Element](https://lit-element.polymer-project.org/guide)

Web components are basically a way of grouping together related HTML markup, Javascript and CSS styles into one, easy
(hopefully) to understand, source file. A date picker is a great example. Usually a date picker involves some Javascript
in an imported file on your page, or inline, some HTML tags on your page, separate from the JS, and some styles,
possibly compiled up somewhere else in your codebase using SASS. Additionally, this pattern may well be repeated again
and again across your tools. So, you make that a web component by bringing together the HTML, Javascript and CSS into
a web component. Web components each add a new tag to your browser, so you just add the HTML tag where you want the date
picker to sit, maybe set a few attributes on the tag, and that's it. So, web components are a great way of cleaning up
your code, reducing repetition and all that.

## Sakai's Web Components

This project, webcomponents, is being used to host the shared components in use across various aspects of the Sakai
codebase. The first one of these is the sakai-tool-permissions component. All of the Sakai components to date are, in
fact, [custom elements](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_custom_elements) in that they
aren't using the [shadow dom](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_shadow_DOM) to hide
their child elements. Making your components custom elements by not using the shadow dom gives us one big advantage - we
can continue to style all the elements in Sakai using our global, SASS generated, stylesheets. You can style fully 
encapsulated web components from outside the shadow dom, but it requires some alterations to the way we build our
styles. Using the custom element approach takes us the first step on the journey.

All of our web components up to now have been written using lit-element. You don't need to, you could just build with
the W3C spec, but lit-element is nice and simple and designed to just be a bridge to the point where the W3C spec is all
you need.
