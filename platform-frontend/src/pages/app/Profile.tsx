import { useState, useEffect } from "react"
import { toast } from "sonner";
import { Save, Trophy } from "lucide-react"

import { InputComponents, DisplayComponents } from '../../components/app'
import { useProfile, useUpdateProfile } from "../../hooks/player/useProfile";
import { useGetAllGames } from '../../hooks/game/useGame'

const { Avatar, AvatarFallback, Card } = DisplayComponents
const { Button, Input, Label, TextArea, DropdownCheckbox } = InputComponents

const DEFAULT_FORM = {
  username: "",
  bio: "",
  address: "",
  gamePreferences: [] as string[],
}

export const Profile = () => {
  const { data: profile, isLoading } = useProfile();
  const { data: gamesResponse } = useGetAllGames()
  const updateProfile = useUpdateProfile();
  const [formData, setFormData] = useState(DEFAULT_FORM);

  const userInitials = profile?.username.slice(0, 2).toUpperCase();

  useEffect(() => {
    if (profile) {
      setFormData({
        username: profile.username || "",
        bio: profile.bio || "",
        address: profile.address || "",
        gamePreferences: profile.gamePreferences || [],
      });
    }
  }, [profile]);

  const handleSave = () => updateProfile.mutateAsync({
        username: formData.username,
        bio: formData.bio,
        address: formData.address,
        gamePreferences: formData.gamePreferences,
      }).then(() => toast.success("Profile updated successfully!"))
      .catch(() => toast.error("Failed to update profile"))

  if (isLoading) {
    return <div className="text-center py-12">Loading profile...</div>;
  }

  if (!profile) {
    return <div className="text-center py-12">Profile not found</div>;
  }

  return (
      <div className="space-y-8">
        <div className="space-y-2">

          <h1 className="tracking-tight">Profile</h1>
          <p className="text-[var(--muted-foreground)]">
            Manage your profile information and preferences
          </p>
        </div>

        <Card className="border border-[var(--border)] bg-[var(--card)] shadow-[var(--shadow-soft)]">
          <div className="px-6 pt-8 pb-6 border-b border-[var(--border)]">
            <div className="flex flex-col items-center text-center gap-3">
              <Avatar className="h-20 w-20 ring-1 ring-[var(--border)] bg-[var(--secondary)]">
                <AvatarFallback className="text-2xl font-semibold text-[var(--foreground)] bg-[var(--secondary)]">
                  {userInitials}
                </AvatarFallback>
              </Avatar>

              <div className="space-y-1">
                <h3 className="text-[var(--foreground)]">{profile.username}</h3>
                <p className="text-sm text-[var(--muted-foreground)]">{profile.email}</p>
              </div>

              <div className="mt-5 w-full rounded-lg border border-[var(--border)] bg-[var(--secondary)]">
                <div
                    className="grid grid-cols-1 sm:grid-cols-2 divide-y sm:divide-y-0 sm:divide-x divide-[var(--border)]">
                  <div className="flex items-center justify-center gap-2 px-4 py-3 text-sm">
                    <Trophy className="h-4 w-4 text-[var(--muted-foreground)]"/>
                    <span className="text-[var(--muted-foreground)]">{profile.rank}</span>
                  </div>

                  <div className="flex items-center justify-center gap-2 px-4 py-3 text-sm">
                    <Trophy className="h-4 w-4 text-[var(--muted-foreground)]"/>
                    <span className="text-[var(--muted-foreground)]">{profile.exp} XP</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="p-6 space-y-8">

            <section className="space-y-4">
              <h3 className="text-[var(--foreground)]">Personal Information</h3>

              <div className="grid gap-5">
                <div className="space-y-2">
                  <Label className="text-base text-[var(--foreground)]">Username</Label>
                  <Input
                      id="username"
                      value={formData.username}
                      onChange={(e) => setFormData((prev) => ({ ...prev, 'username': e.target.value }))}
                      className="bg-[color:var(--color-bg)] border-[var(--border)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus-visible:ring-1 focus-visible:ring-[var(--ring)]"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="address" className="text-base text-[var(--foreground)]">
                    Address
                  </Label>
                    <Input
                        id="address"
                        value={formData.address}
                        onChange={(e) => setFormData((prev) => ({ ...prev, 'address': e.target.value }))}
                        className="bg-[color:var(--color-bg)] border-[var(--border)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus-visible:ring-1 focus-visible:ring-[var(--ring)]"
                    />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="bio" className="text-base text-[var(--foreground)]">
                    Bio
                  </Label>
                  <TextArea
                      id="bio"
                      value={formData.bio}
                      onChange={(e) => setFormData((prev) => ({ ...prev, 'bio': e.target.value }))}
                      rows={4}
                      placeholder="Tell us about yourself..."
                      className="bg-[color:var(--color-bg)] border border-[var(--border)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[var(--ring)] rounded-md"
                  />
                </div>


                <div className="space-y-2">
                  <Label htmlFor="email" className="text-base text-[var(--foreground)]">
                    Email
                  </Label>
                  <Input
                      id="email"
                      type="email"
                      value={profile.email || ""}
                      disabled
                      className="bg-[var(--secondary)] border-[var(--border)] text-[var(--foreground)] opacity-80"
                  />
                </div>
              </div>
            </section>

            <section className="pt-6 border-t border-[var(--border)] space-y-4">
              <h3 className="text-[var(--foreground)]">Gaming Preferences</h3>

              <div className="space-y-2">
                <DropdownCheckbox
                    placeholder="Choose games"
                    items={(gamesResponse?.games ?? []).map((g) => ({ id: String(g.id), text: g.title }))}
                    checkedItemIds={formData.gamePreferences}
                    onValueChange={(next) =>
                        setFormData((prev) => ({ ...prev, gamePreferences: next }))}
                />
              </div>
            </section>

            <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3 pt-2">
              <Button
                  variant="outline"
                  onClick={() => window.location.reload()}
                  className="border-[var(--border)] bg-transparent text-[var(--foreground)] hover:bg-[var(--secondary)]"
              >
                Cancel
              </Button>

              <Button
                  onClick={handleSave}
                  disabled={updateProfile.isPending}
                  className="bg-[var(--primary)] text-[var(--primary-foreground)] hover:bg-[var(--color-accent-hover)] disabled:opacity-60"
              >
                <Save className="w-4 h-4 mr-2"/>
                {updateProfile.isPending ? "Saving..." : "Save Changes"}
              </Button>
            </div>
          </div>
        </Card>
      </div>
  );
}
